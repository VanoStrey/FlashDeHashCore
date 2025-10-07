package customChunk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import coreChunk.ChunkValueEncoding;

public class DictionaryProcessor {

    private static final int BUFFER_SIZE = 8192;

    public static void main(String[] args) {
        Path dictionaryPath = Paths.get("BIG-WPA-LIST-3");
        if (!Files.exists(dictionaryPath)) {
            System.err.println("Файл не найден: " + dictionaryPath);
            return;
        }

        try {
            new DictionaryProcessor().processDictionary(dictionaryPath);
        } catch (IOException e) {
            System.err.println("Ошибка обработки: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processDictionary(Path dictionaryPath) throws IOException {
        System.out.println("Обработка словаря: " + dictionaryPath.getFileName());

        // === 1. Подготовка папки вывода ===
        String baseName = dictionaryPath.getFileName().toString();
        Path outputDir = dictionaryPath.resolveSibling(baseName + "_chunks");
        Files.createDirectories(outputDir);

        // === 2. Анализ частот символов ===
        System.out.println("Подсчёт частот символов...");
        Map<Character, Integer> frequencyMap = analyzeAlphabet(dictionaryPath);

        // === 3. Сортировка символов по частоте ===
        String alphabet = buildAlphabet(frequencyMap);

        System.out.println("Алфавит: " + alphabet);
        System.out.println("Всего уникальных символов: " + alphabet.length());

        // === 4. Разделение словаря по длинам ===
        System.out.println("Разделение словаря на чанки...");
        splitIntoChunks(dictionaryPath, outputDir, new ChunkValueEncoding(alphabet));

        // === 5. Создание metadata.json ===
        System.out.println("Создание metadata.json...");
        createMetadata(outputDir, alphabet);

        System.out.println("✅ Завершено. Результаты сохранены в: " + outputDir);
    }

    /** Подсчёт частоты символов во всём файле */
    private Map<Character, Integer> analyzeAlphabet(Path path) throws IOException {
        Map<Character, Integer> frequencyMap = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            long count = 0;

            while ((line = reader.readLine()) != null) {
                count++;
                String cleaned = line.trim();
                if (cleaned.isEmpty()) continue;

                for (char c : cleaned.toCharArray()) {
                    if (Character.isDefined(c) && !Character.isSurrogate(c)) {
                        frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
                    }
                }

                if (count % 1000000 == 0)
                    System.out.println("  обработано строк: " + count);
            }
        }

        return frequencyMap;
    }

    /** Создание строки алфавита, отсортированной по частоте */
    private String buildAlphabet(Map<Character, Integer> frequencyMap) {
        List<Map.Entry<Character, Integer>> sorted = new ArrayList<>(frequencyMap.entrySet());
        sorted.sort((a, b) -> {
            int cmp = b.getValue().compareTo(a.getValue());
            if (cmp == 0) return a.getKey().compareTo(b.getKey());
            return cmp;
        });

        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Map.Entry<Character, Integer> e : sorted)
            sb.append(e.getKey());
        return sb.toString();
    }

    /** Разделение на чанки по длине слова */
    private void splitIntoChunks(Path dictionaryPath, Path outputDir, ChunkValueEncoding encoding) throws IOException {

        Map<Integer, DataOutputStream> writers = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(dictionaryPath, StandardCharsets.UTF_8)) {
            String line;
            long processed = 0;

            while ((line = reader.readLine()) != null) {
                processed++;
                String cleaned = line.trim();
                if (cleaned.isEmpty()) continue;

                byte[] bytes = encoding.decodeFromString(line);
                int len = bytes.length;

                DataOutputStream out = writers.computeIfAbsent(len, l -> {
                    try {
                        Path file = outputDir.resolve("chunk_" + l + ".bin");
                        return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND), BUFFER_SIZE));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

                out.write(bytes);

                if (processed % 1000000 == 0)
                    System.out.println("  обработано строк: " + processed);
            }
        } finally {
            for (DataOutputStream out : writers.values())
                out.close();
        }
    }

    /** Создание JSON с информацией о словаре */
    private void createMetadata(Path outputDir, String alphabet) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("dictionary_symbols", alphabet);
        meta.put("total_symbols", alphabet.length());
        meta.put("created_at", new Date().toString());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(meta);

        Files.writeString(outputDir.resolve("metadata.json"), json, StandardCharsets.UTF_8);
    }
}
