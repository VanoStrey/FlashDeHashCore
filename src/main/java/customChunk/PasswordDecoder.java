package customChunk;

import coreChunk.ChunkValueEncoding;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PasswordDecoder {
    private final ChunkValueEncoding encoding;
    private Map<Integer, Integer> byteLengthCount = new HashMap<>();
    private int processedLines = 0;
    private int errorCount = 0;

    public PasswordDecoder(String alphabet) {
        this.encoding = new ChunkValueEncoding(alphabet);
    }

    public void processFile(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                processedLines++;

                // Пропускаем пустые строки
                if (line.trim().isEmpty()) continue;

                try {
                    byte[] value = encoding.decodeFromString(line);
                    if (line.equals(encoding.convertToBaseString(value))) {
                        updateByteLengthCount(value.length);
                    } else {
                        System.err.println("Строка №" + lineNumber + ": Некорректный перевод " + line);
                        errorCount++;
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Ошибка обработки строки №" + lineNumber + ": " + e.getMessage());
                    errorCount++;
                }

                // Прогресс каждые 1 000 000 строк
                if (processedLines % 1000000 == 0) {
                    System.out.println("Обработано строк: " + processedLines);
                    printByteLengthStats();
                }
            }

            // Финальный вывод
            System.out.println("\nОбщее количество обработанных строк: " + processedLines);
            System.out.println("Количество ошибок: " + errorCount);
            printByteLengthStats();

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }
    }

    private void updateByteLengthCount(int byteLength) {
        byteLengthCount.put(byteLength, byteLengthCount.getOrDefault(byteLength, 0) + 1);
    }
    
    private void printByteLengthStats() {
        System.out.println("\nСтатистика по длине в байтах:");

        // Создаем список записей и сортируем их по ключу
        List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(byteLengthCount.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        long totalBytes = 0; // Переменная для подсчета общего веса

        // Выводим статистику по каждой длине
        for (Map.Entry<Integer, Integer> entry : sortedEntries) {
            System.out.printf("Байты: %d - Количество: %d%n", entry.getKey(), entry.getValue());
            totalBytes += entry.getKey() * entry.getValue(); // Считаем общий вес
        }

        // Добавляем вывод общей статистики
        System.out.println("\nОбщая статистика:");
        System.out.printf("Общий вес всех данных: %d байт%n", totalBytes);
        System.out.printf("Общий вес в килобайтах: %.2f KB%n", totalBytes / 1024.0);
        System.out.printf("Общий вес в мегабайтах: %.2f MB%n", totalBytes / (1024.0 * 1024.0));
        System.out.printf("Общий вес в гигабайтах: %.2f GB%n", totalBytes / (1024.0 * 1024.0 * 1024.0));
        for (int i = 0; i < 40; i++) {
            System.out.print("-");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        String alphabet = " eaisnrtol6ducgmhbpkE5ywvfI7DCzjGBHS1RN340A92F8TOLxqMUPKVYWZJXQ";
        PasswordDecoder decoder = new PasswordDecoder(alphabet);

        try {
            decoder.processFile("BIG-WPA-LIST-3");
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
        }
    }
}
