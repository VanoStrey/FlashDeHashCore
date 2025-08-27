package coreChunk;

import hashFunc.Hasher;
import hashFunc.SHA256Hash;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Корректное слияние 256 трехбайтных чанков в 256 четырёхбайтных,
 * строго соблюдая глобальный порядок по SHA-256.
 *
 * Архитектура сохранена:
 *  - вычисление глобального [minHash, maxHash];
 *  - деление диапазона на 256 рангов (hashRanks);
 *  - для каждого входного чанка определяем границы каждого ранга (binary search);
 *  - для каждого выходного чанка j выполняем k-way merge подотрезков всех 256 входных чанков.
 */
public class Convert3bChunkTo4b {

    private static final int OUT_BUFFER_BYTES = 16 * 1024 * 1024; // 16MB

    public static void main(String[] args) throws Exception {

        Hasher hasher = new SHA256Hash();
        String dictionarySymbols = "0123456789";
        ChunkValueEncoding encoding = new ChunkValueEncoding(dictionarySymbols);

        // ---------- 1) Глобальные min/max ----------
        byte[] hashMax = null;
        for (int i = 256; i < 512; i++) {
            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);
            byte[] last = accessor.getElement(accessor.getTotalElements() - 1);
            byte[] maxHash = hasher.getBinHash(encoding.convertToBaseString(last));
            if (hashMax == null || compareHashes(hashMax, maxHash) < 0) hashMax = maxHash;
        }
        System.out.println("GLOBAL MAX = " + hasher.bytesToHex(hashMax));

        byte[] hashMin = null;
        for (int i = 256; i < 512; i++) {
            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);
            byte[] first = accessor.getElement(0);
            byte[] minHash = hasher.getBinHash(encoding.convertToBaseString(first));
            if (hashMin == null || compareHashes(hashMin, minHash) > 0) hashMin = minHash;
        }
        System.out.println("GLOBAL MIN = " + hasher.bytesToHex(hashMin));

        // ---------- 2) Делим диапазон на 256 рангов ----------
        BigInteger bigMax = new BigInteger(1, hashMax);
        BigInteger bigMin = new BigInteger(1, hashMin);

        BigInteger diff = bigMax.subtract(bigMin);
        BigInteger step = diff.divide(BigInteger.valueOf(256));

        System.out.println("Δ (hex) = " + diff.toString(16));
        System.out.println("step (hex) = " + step.toString(16));
        System.out.println();

        ArrayList<String> hashRanks = new ArrayList<>();
        BigInteger cursor = bigMin;
        for (int i = 0; i < 255; i++) {
            cursor = cursor.add(step);
            StringBuilder outHash = new StringBuilder(cursor.toString(16));
            while (outHash.length() != hasher.getHash("").length()) outHash.insert(0, "0");
            hashRanks.add(outHash.toString());
        }
        hashRanks.add(hasher.bytesToHex(hashMax)); // последний ранг заканчивается глобальным максимумом

        // (опционально) вывести ранги
        System.out.println(hashRanks.size());
        for (String hashRank : hashRanks) System.out.println(hashRank);

        // ---------- 3) Границы рангов для КАЖДОГО из 256 входных чанков ----------
        // ranksIndexesChunks[i] = [0, idx_0, idx_1, ..., idx_255] для чанка i
        ArrayList<ArrayList<Long>> ranksIndexesChunks = new ArrayList<>();

        for (int i = 256; i < 512; i++) {
            ArrayList<Long> ranksIndexes = new ArrayList<>();
            ranksIndexes.add(0L); // старт
            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);

            for (String hashRank : hashRanks) {
                byte[] targetHash = hasher.hexToBytes(hashRank);
                long index = findLastLessOrEqual(accessor, hasher, encoding, targetHash);
                ranksIndexes.add(index);
            }
            ranksIndexesChunks.add(ranksIndexes);
            //System.out.println("chunk " + i + " bounds(size = " + ranksIndexes.size() + "): " + ranksIndexes);
        }

        // ---------- 4) Для каждого выходного чанка j делаем k-way merge ----------
        Path outPath = Paths.get("chunks4_SHA256_[0-9]", "chunk_1.bin");
        Path folder = outPath.getParent();
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            System.out.println("📁 Папка создана: " + folder.toAbsolutePath());
        }

// Открываем один общий выходной поток (append!)
        try (BufferedOutputStream os = new BufferedOutputStream(
                Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND),
                OUT_BUFFER_BYTES
        )) {

            // hashRanks.size() == 256, индексы в ranksIndexesChunks — 0..256
            for (int j = 1; j < ranksIndexesChunks.getFirst().size(); j++) {
                int outIndex = j - 1;
                System.out.println("➡️  Сборка блока #" + outIndex);

                // Мин-куча по (hash, raw)
                PriorityQueue<Node> heap = new PriorityQueue<>(Comparator
                        .comparing((Node n) -> n.hash, Convert3bChunkTo4b::compareHashesLex)
                        .thenComparing(n -> n.raw, Convert3bChunkTo4b::compareRawLex));

                // Откроем все 256 входных чанков и инициализируем по 1 элементу из их подотрезка [start,end)
                ChunkBinaryFileAccessor[] accessors = new ChunkBinaryFileAccessor[256];
                long totalRangeCount = 0;

                for (int i = 0; i < 256; i++) {
                    long start = ranksIndexesChunks.get(i).get(j - 1);
                    long end = ranksIndexesChunks.get(i).get(j);
                    totalRangeCount += Math.max(0, end - start);

                    if (start < end) {
                        accessors[i] = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + (i + 256) + ".bin", 3);
                        long pos = start;

                        byte[] raw = accessors[i].getElement(pos);
                        byte[] hash = hasher.getBinHash(encoding.convertToBaseString(raw));

                        heap.add(new Node(i, pos, end, raw, hash));
                    }
                }

                System.out.println("   подотрезков не пустых: " + heap.size() + " / 256; элементов ≈ " + totalRangeCount);

                // Слияние + запись
                long written = 0;
                while (!heap.isEmpty()) {
                    Node n = heap.poll();
                    os.write(n.raw);
                    written++;

                    // продвигаем этот же входной чанк
                    long nextPos = n.pos + 1;
                    if (nextPos < n.end) {
                        byte[] el = accessors[n.chunkIdx].getElement(nextPos);
                        byte[] raw = Arrays.copyOfRange(el, 2, 6);
                        byte[] hash = hasher.getBinHash(encoding.convertToBaseString(raw));
                        heap.add(new Node(n.chunkIdx, nextPos, n.end, raw, hash));
                    }
                }

                System.out.println("✅ блок #" + outIndex + " готов. Элементов: " + written + "\n");
            }
        }

        System.out.println("🎉 Все блоки собраны в один файл chunk_0.bin");
    }

    // Узел для кучи
    private static final class Node {
        final int chunkIdx;
        final long pos;     // текущая позиция в исходном чанке
        final long end;     // конец подотрезка (исключительно)
        final byte[] raw;   // 4 байта
        final byte[] hash;  // полный SHA-256

        Node(int chunkIdx, long pos, long end, byte[] raw, byte[] hash) {
            this.chunkIdx = chunkIdx;
            this.pos = pos;
            this.end = end;
            this.raw = raw;
            this.hash = hash;
        }
    }

    // --------- Сравнения ---------

    // Лексикографическое сравнение 4 байт как UNSIGNED
    private static int compareRaw(byte[] a, byte[] b) {
        for (int i = 0; i < 4; i++) {
            int ai = a[i] & 0xFF;
            int bi = b[i] & 0xFF;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    // Компаратор для byte[] как для "беззнаковых" байтов
    private static int compareHashes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int ai = a[i] & 0xFF, bi = b[i] & 0xFF;
            if (ai != bi) return ai - bi;
        }
        return a.length - b.length;
    }

    // Для PriorityQueue удобно иметь Comparator<byte[]>
    private static int compareHashesLex(byte[] a, byte[] b) {
        return compareHashes(a, b);
    }

    private static int compareRawLex(byte[] a, byte[] b) {
        return compareRaw(a, b);
    }

    // --------- Вспомогательные из твоего кода ---------

    private static long findLastLessOrEqual(ChunkBinaryFileAccessor accessor, Hasher hasher, ChunkValueEncoding encoding, byte[] targetHash) throws IOException {
        long left = 0;
        long right = accessor.getTotalElements() - 1;
        long result = -1;

        while (left <= right) {
            long mid = left + (right - left) / 2;
            byte[] currentHash = hasher.getBinHash(
                    encoding.convertToBaseString(accessor.getElement(mid))
            );

            int comparison = compareHashes(currentHash, targetHash);
            if (comparison <= 0) {
                result = mid;      // mid подходит (<= target), двигаемся вправо
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return ++result; // количество элементов ≤ target
    }

}
