package coreChunk;

import hashFunc.Hasher;
import hashFunc.HasherFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DictionarySearch {
    public ArrayList<HashBinarySearch> hashBinarySearch = new ArrayList<>();
    private final String dictionaryDir;
    public ChunkValueEncoding converter;
    public Hasher hasher;
    private final boolean printLog;
    private final long RANGE = 8192;
    
    // Кэшированный пул потоков
    private final ExecutorService executor;
    private static final ThreadFactory THREAD_FACTORY = r -> {
        Thread t = new Thread(r, "DictionarySearch-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    };

    public DictionarySearch(String dictionaryDir, boolean printLog) throws IOException {
        Path metadataPath = Paths.get(dictionaryDir, "metadata.json");
        String metadataContent = Files.readString(metadataPath);
        JSONObject meta = new JSONObject(metadataContent);

        this.converter = new ChunkValueEncoding(meta.getString("dictionary_symbols"));
        this.hasher = HasherFactory.getHasher(meta.getString("hash_algorithm"));
        this.dictionaryDir = dictionaryDir;
        this.printLog = printLog;

        initChunkSearch();
        
        // Создаем пул потоков ПОСЛЕ загрузки чанков, чтобы знать их количество
        int processors = Runtime.getRuntime().availableProcessors();
        int chunkCount = hashBinarySearch.size();
        
        // Используем неограниченную очередь, чтобы избежать проблемы с емкостью 0
        this.executor = new ThreadPoolExecutor(
            processors, // corePoolSize
            processors, // maximumPoolSize
            60L, TimeUnit.SECONDS, // keepAliveTime
            new LinkedBlockingQueue<>(), // Без ограничения емкости
            THREAD_FACTORY,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    public String search(String hash) throws InterruptedException {
        if (hash.length() != hasher.getHash("").length()) {
            return "invalid hash type, use " + hasher.getName();
        }

        byte[] target = hasher.hexToBytes(hash);
        
        if (printLog) System.out.println("🔍 Поиск хеша: " + hash);

        double startTime = System.nanoTime();

        // первые 3 байта хеша сами по себе являются индексом для пресказания расположения в чанке
        long predicted = ((target[0] & 0xFFL) << 16) |
                ((target[1] & 0xFFL) << 8) |
                (target[2] & 0xFFL);

        long low = Math.max(0, predicted - RANGE);
        long high = Math.min((1L << 24) - 1, predicted + RANGE);

        if (printLog) System.out.println("📍 Единое предсказание для всех чанков: " + predicted);

        // Используем существующий executor вместо создания нового
        List<Future<SearchResult>> futures = new ArrayList<>();
        AtomicBoolean found = new AtomicBoolean(false);

        for (int i = 0; i < hashBinarySearch.size(); i++) {
            final int currentChunk = i;

            futures.add(executor.submit(() -> {
                if (found.get()) return new SearchResult(currentChunk, "");
                try {
                    String result = hashBinarySearch.get(currentChunk).search(hash, low, high);
                    if (!result.isEmpty()) {
                        found.set(true);
                        return new SearchResult(currentChunk, result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new SearchResult(currentChunk, "");
            }));
        }

        // НЕ закрываем executor после использования!

        for (Future<SearchResult> future : futures) {
            try {
                SearchResult result = future.get();
                if (result != null && !result.result.isEmpty()) {
                    double totalTime = System.nanoTime() - startTime;
                    if (printLog) {
                        System.out.println("✅ Найдено в чанке " + result.chunkIndex +
                                         " за " + String.format("%.2f", totalTime / 1_000_000.0) + " ms");
                    }
                    return result.result;
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (printLog) System.out.println("❌ Хеш не найден за " + (System.nanoTime() - startTime / 1_000_000.0) + " ms");
        return "hash not found";
    }

    private void initChunkSearch() throws IOException {
        Integer elementSize = 3;
        if (printLog) {
            System.out.println("🧠 Инициализация и прогрев чанков...");
        }

        for (Path chunkPath : findAllChunkPaths()) {
            String path = chunkPath.toString();

            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor(path, elementSize);
            HashBinarySearch search = new HashBinarySearch(accessor, converter, hasher);
            hashBinarySearch.add(search);

            long total = accessor.getTotalElements();
            long[] offsets = {
                    0,
                    total / 4,
                    total / 2,
                    (3 * total) / 4,
                    Math.max(0, total - 1)
            };

            for (long offset : offsets) {
                byte[] el = accessor.getElement(offset);
                if (el == null) continue;
                String encoded = converter.convertToBaseString(el);
                byte[] hashBytes = hasher.getBinHash(encoded);
                hashBytes[0] ^= el[0]; // Фиктивная операция для прогрева
            }
        }

        System.gc();
        if (printLog) {
            System.out.println("✅ Система прогрета и готова к работе\n");
        }
    }

    private List<Path> findAllChunkPaths() throws IOException {
        try (var stream = Files.list(Paths.get(dictionaryDir))) {
            return stream
                    .filter(path -> path.getFileName().toString().matches("chunk_\\d+\\.bin"))
                    .sorted(Comparator.comparingInt(p ->
                            Integer.parseInt(p.getFileName().toString()
                                    .replace("chunk_", "")
                                    .replace(".bin", "")))
                    )
                    .toList();
        }
    }

    private static class SearchResult {
        final int chunkIndex;
        final String result;
        
        SearchResult(int chunkIndex, String result) {
            this.chunkIndex = chunkIndex;
            this.result = result;
        }
    }
    
    // Добавим метод shutdown для корректного завершения приложения
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}