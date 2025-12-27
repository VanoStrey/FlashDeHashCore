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

    public DictionarySearch(String dictionaryDir, boolean printLog) throws IOException {
        Path metadataPath = Paths.get(dictionaryDir, "metadata.json");
        String metadataContent = Files.readString(metadataPath);
        JSONObject meta = new JSONObject(metadataContent);

        String symbols = meta.getString("dictionary_symbols");
        String hashAlg = meta.getString("hash_algorithm");
        Integer elementSize = meta.getInt("element_size");

        this.converter = new ChunkValueEncoding(symbols);
        this.hasher = HasherFactory.getHasher(hashAlg);
        this.dictionaryDir = dictionaryDir;
        this.printLog = printLog;

        initChunkSearch(elementSize);
        
        if (printLog) {
            System.out.println("⚡ Оптимизация: одна интерполяция для всех чанков");
        }
    }

    public String search(String hash) throws InterruptedException {
        if (hash.length() != hasher.getHash("12345").length()) {
            return "invalid hash type, use " + hasher.getName();
        }

        byte[] target = hasher.hexToBytes(hash);
        int totalChunks = hashBinarySearch.size();
        
        if (printLog) {
            System.out.println("🔍 Поиск хеша: " + hash);
        }

        long startTime = System.nanoTime();
        
        try {
            // 1. ВЫЧИСЛЯЕМ ПРЕДСКАЗАНИЕ ОДИН РАЗ (используем первый чанк как образец)
            // Первый чанк уже имеет вычисленные lowHash и highHash
            HashBinarySearch firstSearch = hashBinarySearch.get(0);
            
            // Вычисляем предсказание по формуле из HashBinarySearch.predictIndex()
            long predicted = firstSearch.predictPosition(target);
            
            if (printLog) {
                System.out.println("📍 Единое предсказание для всех чанков: " + predicted);
            }

            // 2. Параллельный поиск во всех чанках с ОДНИМ предсказанием
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<SearchResult>> futures = new ArrayList<>();
            AtomicBoolean found = new AtomicBoolean(false);
            
            for (int chunkIdx = 0; chunkIdx < totalChunks; chunkIdx++) {
                final int currentChunk = chunkIdx;
                
                futures.add(executor.submit(() -> {
                    if (found.get()) return new SearchResult(currentChunk, "");
                    
                    try {
                        // Используем метод с переданным предсказанием
                        String result = hashBinarySearch.get(currentChunk).searchWithPrediction(hash, predicted);
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
            
            executor.shutdown();
            
            // 3. Собираем результаты
            for (Future<SearchResult> future : futures) {
                try {
                    SearchResult result = future.get();
                    if (result != null && !result.result.isEmpty()) {
                        long totalTime = System.nanoTime() - startTime;
                        if (printLog) {
                            System.out.println("✅ Найдено в чанке " + result.chunkIndex + 
                                             " за " + (totalTime / 1_000_000.0) + " мс");
                        }
                        return result.result;
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        long totalTime = System.nanoTime() - startTime;
        if (printLog) {
            System.out.println("❌ Хеш не найден за " + (totalTime / 1_000_000.0) + " мс");
        }
        return "hash not found";
    }

    private void initChunkSearch(Integer elementSize) throws IOException {
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
}