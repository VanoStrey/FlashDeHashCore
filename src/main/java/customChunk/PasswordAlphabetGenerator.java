package customChunk;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class PasswordAlphabetGenerator {
    public static void main(String args[]) {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        long processedLines = 0;

        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get("BIG-WPA-LIST-3"), StandardCharsets.UTF_8)) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                processedLines++;

                try {
                    // Проверяем, не пустая ли строка
                    if (line.isEmpty()) {
                        System.err.println("Пропущена пустая строка " + lineNumber);
                        continue;
                    }

                    // Проверяем длину строки
                    if (line.length() == 1) {
                        System.err.println("Пропущена строка с одним символом: " + line + " (строка " + lineNumber + ")");
                        continue;
                    }

                    // Очищаем строку и обрабатываем
                    String cleanedLine = line.trim();
                    if (cleanedLine.isEmpty()) continue;

                    for (char c : cleanedLine.toCharArray()) {
                        if (Character.isDefined(c) && !Character.isSurrogate(c)) {
                            frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
                        }
                    }

                    // Вывод промежуточного результата каждые 100 000 строк
                    if (processedLines % 100000 == 0) {
                        System.out.println("\nОбработано строк: " + processedLines);
                        printIntermediateAlphabet(frequencyMap);
                    }

                } catch (Exception e) {
                    System.err.println("Ошибка обработки строки " + lineNumber + ": " + e.getMessage());
                    System.err.println("Проблемная строка: " + line);
                }
            }

            // Финальный вывод
            System.out.println("\nОбщее количество обработанных строк: " + processedLines);
            printIntermediateAlphabet(frequencyMap);

        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    private static void printIntermediateAlphabet(Map<Character, Integer> frequencyMap) {
        // Сортируем символы по частоте
        List<Map.Entry<Character, Integer>> sortedEntries = new ArrayList<>(frequencyMap.entrySet());
        sortedEntries.sort((e1, e2) -> {
            int freqCompare = e2.getValue().compareTo(e1.getValue());
            if (freqCompare == 0) {
                return e1.getKey().compareTo(e2.getKey());
            }
            return freqCompare;
        });

        // Формируем алфавит
        StringBuilder customAlphabet = new StringBuilder();
        for (Map.Entry<Character, Integer> entry : sortedEntries) {
            customAlphabet.append(entry.getKey());
        }

        System.out.println("Промежуточный алфавит:");
        System.out.println(customAlphabet.toString());

        // Выводим топ-50 символов
        System.out.println("\nТоп-50 самых частых символов:");
        for (int i = 0; i < Math.min(50, sortedEntries.size()); i++) {
            Map.Entry<Character, Integer> entry = sortedEntries.get(i);
            System.out.printf("%c: %d раз%n", entry.getKey(), entry.getValue());
        }
    }
}
