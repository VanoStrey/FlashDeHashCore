package benchmark;

import coreChunk.*;
import hashFunc.*;

import java.io.IOException;
import java.lang.management.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;

public class BatchPerformanceMonitor {

    private static final String outputDir = "chunks_CSHAKE128_[0-9]";
    private static final int TEST_HASH_COUNT = 1000;
    private static final boolean printHashResult = false;
    private static final int comboMaxLength = 6;
    private static final DecimalFormat fmt = new DecimalFormat("0.00");

    public static void main(String[] args) throws Exception {

        DictionarySearch dictionarySearch = new DictionarySearch(outputDir, false);
        Hasher hasher = dictionarySearch.hasher;
        String alphabet = dictionarySearch.converter.getRangeChars();

        long dictSize = dictionarySearch.hashBinarySearch.size();

        // Вычисляем 2^24 точно
        BigInteger powerOfTwo = BigInteger.valueOf(2).pow(24);

        // Умножаем на размер словаря
        System.out.println("Словарь : " + outputDir);
        System.out.println("Колличество уникальных комбинаций в словаре : " + BigInteger.valueOf(dictSize).multiply(powerOfTwo));
        String maxCombo = "";
        for (int i = 0; i < comboMaxLength; i++) {
            maxCombo += alphabet.charAt(alphabet.length()-1);
        }
        System.out.println("Тестовые рандомные комбинации от \"" + alphabet.charAt(0) + "\" до \"" + maxCombo + "\"");
        List<String> testHashes = generateTestHashes(hasher, alphabet, TEST_HASH_COUNT);
        System.out.println("⚙\uFE0F Хеши для тестирования сгенерированы\n");
        System.gc();
        Thread.sleep(100);
        Runtime runtime = Runtime.getRuntime();
        ThreadMXBean tm = ManagementFactory.getThreadMXBean();
        if (tm.isThreadCpuTimeSupported() && !tm.isThreadCpuTimeEnabled()) {
            tm.setThreadCpuTimeEnabled(true);
        }

        Map<Long, Long> cpuBefore = snapshotCpuTime(tm);

        double totalWall = 0;
        double totalRam = 0;
        int lossHash = 0;

        for (String hash : testHashes) {
            System.gc(); Thread.sleep(1); // минимальная стабилизация
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            long start = System.nanoTime();

            String result = dictionarySearch.search(hash);
            if (printHashResult){
                System.out.println(hash + ": " + result);
            }

            long end = System.nanoTime();
            long memAfter = runtime.totalMemory() - runtime.freeMemory();

            double wallMs = (end - start) / 1_000_000.0;
            double ramMb = (memAfter - memBefore) / 1024.0 / 1024;

            totalWall += wallMs;
            totalRam += ramMb;
            if (result.equals("hash not found")) lossHash++;
        }
        Map<Long, Long> cpuAfter = snapshotCpuTime(tm);

        double avgWall = totalWall / TEST_HASH_COUNT;
        double avgRam = totalRam / TEST_HASH_COUNT;

        System.out.println("\nНенайдено " + lossHash + " хешей");
        System.out.println("\n📊 Средняя нагрузка при расшифровке " + TEST_HASH_COUNT + " хэшей:");
        System.out.println("⏱ Среднее время подбора: " + fmt.format(avgWall) + " мс");
        System.out.println("📉 Среднее RAM на хеш: " + fmt.format(avgRam) + " МБ");

        System.out.println("\n🔧 CPU-время по потокам:");
        for (long id : cpuAfter.keySet()) {
            long delta = cpuAfter.get(id) - cpuBefore.getOrDefault(id, 0L);
            if (delta > 1_000_000) {
                ThreadInfo info = tm.getThreadInfo(id);
                if (info != null) {
                    System.out.printf("- [%d] %s: %s мс%n", id, info.getThreadName(), fmt.format(delta / 1_000_000.0));
                }
            }
        }

        long totalSize = Files.walk(Path.of(outputDir))
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try { return Files.size(p); } catch (IOException e) { return 0L; }
                }).sum();

        System.out.printf("\n📂 Размер словаря: %.2f ГБ%n", totalSize / 1024.0 / 1024 / 1024);
    }

    private static List<String> generateTestHashes(Hasher hasher, String alphabet, int count) {
        List<String> hashes = new ArrayList<>();
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < count; i++) {
            int len = rand.nextInt(comboMaxLength);
            StringBuilder sb = new StringBuilder();
            sb.append(alphabet.charAt(rand.nextInt(alphabet.length()-1) + 1));
            for (int j = 0; j < len; j++) {
                sb.append(alphabet.charAt(rand.nextInt(alphabet.length())));
            }
            hashes.add(hasher.getHash(sb.toString()));
        }
        return hashes;
    }

    private static Map<Long, Long> snapshotCpuTime(ThreadMXBean tm) {
        Map<Long, Long> cpu = new HashMap<>();
        for (long id : tm.getAllThreadIds()) {
            long time = tm.getThreadCpuTime(id);
            if (time > 0) cpu.put(id, time);
        }
        return cpu;
    }
}