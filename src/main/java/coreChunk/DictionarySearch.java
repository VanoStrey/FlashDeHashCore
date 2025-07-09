package coreChunk;

import hashFunc.Hasher;
import hashFunc.HasherFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class DictionarySearch {
    private ArrayList<HashBinarySearch> hashBinarySearch = new ArrayList<>();
    private String dictionaryDir;
    private ChunkValueEncoding converter;
    private Hasher hasher;
    private AtomicReference<Integer> chunkIndex = new AtomicReference<>(-1); // Для хранения индекса чанка

    public DictionarySearch(String dictionaryDir) throws IOException {
        Path metadataPath = Paths.get(dictionaryDir, "metadata.json");
        String metadataContent = Files.readString(metadataPath);
        JSONObject meta = new JSONObject(metadataContent);

        String symbols = meta.getString("dictionary_symbols");
        String hashAlg = meta.getString("hash_algorithm");

        this.converter = new ChunkValueEncoding(symbols);
        this.hasher = HasherFactory.getHasher(hashAlg);
        this.dictionaryDir = dictionaryDir;

        initChunkSearch();
    }

    public String search(String hash) throws InterruptedException {
        if (hash.length() != 64){
            return "invalid hash type";
        }
        AtomicReference<String> foundResult = new AtomicReference<>();
        AtomicReference<Integer> chunkIndex = new AtomicReference<>(-1); // Для хранения индекса чанка
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        System.out.println("🔍 Начинаем поиск хеша: " + hash);

        // Запускаем параллельные задачи для поиска в чанках
        for (int i = 0; i < hashBinarySearch.size(); i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                if (foundResult.get() != null) return; // Если результат уже найден, завершаем задачу
                String result = null;
                try {
                    result = hashBinarySearch.get(index).search(hash); // Ищем в чанке
                    if (result != null && !result.isEmpty()) {
                        foundResult.compareAndSet(null, result); // Сохраняем результат
                        chunkIndex.compareAndSet(-1, index); // Сохраняем индекс чанка, где был найден хеш
                        System.out.println("✅ Хеш найден в чанке: chunk_" + index + ".bin");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // Ждём завершения всех потоков
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        // Проверка на успешный поиск и корректный индекс
        if (foundResult.get() != null && chunkIndex.get() != -1) { // Если результат найден и индекс валиден
            int currentChunkIndex = chunkIndex.get(); // Сохраняем текущий индекс
            chunkIndex.set(-1); // Сбрасываем индекс чанка для следующего поиска
            return foundResult.get() + "\n\n🔍 Found in chunk_" + currentChunkIndex + ".bin";
        } else {
            return "hash not found";  // Если хеш не найден, выводим это сообщение
        }
    }


    public String getFoundChunkInfo() {
        return "chunk_" + chunkIndex.get() + ".bin";  // Возвращаем название чанка
    }

    private void initChunkSearch() throws IOException {
        System.out.println("🧠 Инициализация и прогрев чанков...");

        for (Path chunkPath : findAllChunkPaths()) {
            String path = chunkPath.toString();

            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor(path);
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
                byte[] hash = hasher.getBinHash(encoded);
                hash[0] ^= el[0];
            }
        }

        System.gc();
        System.out.println("✅ Система прогрета и готова к работе\n");
    }

    private List<Path> findAllChunkPaths() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dictionaryDir))) {
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
}
