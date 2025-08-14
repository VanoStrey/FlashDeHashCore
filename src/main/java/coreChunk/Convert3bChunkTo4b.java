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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;


public class Convert3bChunkTo4b {
    public static void main(String[] args) throws Exception {

        Hasher hasher = new SHA256Hash();
        String dictionarySymbols = "0123456789";
        ChunkValueEncoding encoding = new ChunkValueEncoding(dictionarySymbols);
        byte[] hash1 = null;
        for (int i = 0; i < 256; i++) {
            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);
            byte[] maxHash = hasher.getBinHash(encoding.convertToBaseString(accessor.getElement(accessor.getTotalElements() - 1)));
            if (hash1 == null || compareHashes(hash1, maxHash) < 0) {
                hash1 = maxHash;
            }
        }
        System.out.println(hasher.bytesToHex(hash1));
        byte[] hash2 = null;
        for (int i = 0; i < 256; i++) {
            ChunkBinaryFileAccessor accessor = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);
            byte[] minHash = hasher.getBinHash(encoding.convertToBaseString(accessor.getElement(0)));
            if (hash2 == null || compareHashes(hash2, minHash) > 0) {
                hash2 = minHash;
            }
        }
        System.out.println(hasher.bytesToHex(hash2));

        BigInteger bigInt1 = new BigInteger(1, hash1);
        BigInteger bigInt2 = new BigInteger(1, hash2);

        BigInteger difference = bigInt1.subtract(bigInt2);
        BigInteger result = difference.divide(BigInteger.valueOf(256));

        System.out.println("Разница: " + difference.toString(16));
        System.out.println("Результат деления на 256: " + result.toString(16));
        System.out.println();

        ArrayList<String> hashRanks = new ArrayList<>();
        for (int i = 0; i < 255; i++) {
            bigInt2 = bigInt2.add(result);
            StringBuilder outHash = new StringBuilder(bigInt2.toString(16));
            while (outHash.length() != hasher.getHash("").length()) {
                outHash.insert(0, "0");
            }
            hashRanks.add(outHash.toString());
        }
        hashRanks.add(hasher.bytesToHex(hash1));

        for (String hashRank : hashRanks) {
            System.out.println(hashRank);
        }

        ArrayList<ArrayList<Long>> ranksIndexesChunks = new ArrayList<>();

        for (int i = 0; i < 256; i++) {
            ArrayList<Long> ranksIndexes = new ArrayList<>();
            ranksIndexes.add(0L);
            ChunkBinaryFileAccessor accessorChunk0 = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);

            for (String hashRank : hashRanks) {
                byte[] targetHash = hasher.hexToBytes(hashRank);
                long index = findLastLessOrEqual(accessorChunk0, hasher, encoding, targetHash);
                ranksIndexes.add(index);
            }
            ranksIndexesChunks.add(ranksIndexes);
            System.out.println(ranksIndexes);
        }


        ArrayList<Long> totalElementsChunks = getDiffLongs(ranksIndexesChunks);
        System.out.println(totalElementsChunks);

        ArrayList<Entry> chunk4b = new ArrayList<>();

        for(int i = 0; i < ranksIndexesChunks.size(); i++){
            ChunkBinaryFileAccessor accessorChunk0 = new ChunkBinaryFileAccessor("chunks_SHA256_[0-9]/chunk_" + i + ".bin", 3);
            System.out.println(i);
            int j = 1;
            for (Long k = ranksIndexesChunks.get(i).get(j - 1); k < ranksIndexesChunks.get(i).get(j); k++) {
                chunk4b.add(new Entry(Arrays.copyOfRange(accessorChunk0.getElement(k), 2, 6)));
            }
        }
        System.out.println(chunk4b.size());
        Entry[] chunk4bArray = chunk4b.toArray(new Entry[0]);
        System.out.println(chunk4bArray.length);

        for (int i = 0; i < chunk4bArray.length; i++) {
            String encoded = encoding.convertToBaseString(chunk4bArray[i].raw);
            chunk4b.get(i).hash = hasher.getBinHash(encoded);
        }
        System.out.println(hasher.bytesToHex(chunk4bArray[chunk4bArray.length - 1].hash));
        quickSort(chunk4bArray, 0, chunk4bArray.length - 1);
        System.out.println(hasher.bytesToHex(chunk4b.get(chunk4b.size() - 1).hash));


        Path outPath = Paths.get("chunks4_SHA256_[0-9]", "chunk_" + 1 + ".bin");
        Path folder = outPath.getParent();
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            System.out.println("📁 Папка создана: " + folder.toAbsolutePath());
        }

        System.out.println("💾 Сохранение в: " + outPath.toAbsolutePath());

        // ⚡ Ускоренная запись с буфером 16 МБ
        try (BufferedOutputStream os = new BufferedOutputStream(
                Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                16 * 1024 * 1024 // 16 МБ буфер
        )) {
            for (Entry e : chunk4bArray) {
                os.write(e.raw);
            }
        }
    }


    private static void quickSort(Entry[] arr, int low, int high) {
        // Создаем стек для хранения границ подмассивов
        Deque<Integer> stack = new ArrayDeque<>();

        // Инициализируем стек начальными границами
        stack.push(low);
        stack.push(high);

        // Пока стек не пуст
        while (!stack.isEmpty()) {
            // Извлекаем границы текущего подмассива
            high = stack.pop();
            low = stack.pop();

            // Находим точку разделения
            int pi = partition(arr, low, high);

            // Если есть подмассив слева от точки разделения
            if (pi - 1 > low) {
                stack.push(low);
                stack.push(pi - 1);
            }

            // Если есть подмассив справа от точки разделения
            if (pi + 1 < high) {
                stack.push(pi + 1);
                stack.push(high);
            }
        }
    }


    private static int partition(Entry[] arr, int low, int high) {
        Entry pivot = arr[high];
        int i = low - 1;

        for (int j = low; j < high; j++) {
            int cmp = compareHashes(arr[j].hash, pivot.hash);
            if (cmp < 0 || (cmp == 0 && compareRaw(arr[j].raw, pivot.raw) <= 0)) {
                i++;
                Entry temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        Entry temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;

        return i + 1;
    }

    private static int compareRaw(byte[] a, byte[] b) {
        for (int i = 0; i < 4; i++) {
            int ai = a[i] & 0xFF;
            int bi = b[i] & 0xFF;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static ArrayList<Long> getDiffLongs(ArrayList<ArrayList<Long>> ranksIndexesChunks) {
        ArrayList<Long> totalElementsChunks = new ArrayList<>();
        for (int i = 1; i < ranksIndexesChunks.getFirst().size(); i++) {
            Long totalElements = 0L;
            for (ArrayList<Long> ranksIndexesChunk : ranksIndexesChunks) {
                totalElements += ranksIndexesChunk.get(i);
            }
            totalElementsChunks.add(totalElements);
        }
        for (int i = totalElementsChunks.size() - 2; i >= 0; i--) {
            totalElementsChunks.set(i + 1, totalElementsChunks.get(i + 1) - totalElementsChunks.get(i));
        }
        return totalElementsChunks;
    }

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
            if (comparison <= 0) { // текущий хеш <= целевой
                result = mid; // запоминаем позицию
                left = mid + 1; // ищем дальше справа
            } else {
                right = mid - 1;
            }
        }
        return ++result;
    }

    private static int compareHashes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int ai = a[i] & 0xFF, bi = b[i] & 0xFF;
            if (ai != bi) return ai - bi;
        }
        return a.length - b.length;
    }

    private static class Entry {
        final byte[] raw;
        byte[] hash;
        Entry(byte[] raw) { this.raw = raw; }
    }
}
