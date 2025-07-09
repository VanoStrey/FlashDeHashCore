package coreChunk;

import hashFunc.Hasher;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HashSortedChunkBuilder {
    private final Entry[] masterEntries;
    private final int elementSize = 3;
    private final int totalElements;
    private final Hasher hasher;
    private final ChunkValueEncoding converter;

    public HashSortedChunkBuilder(
            String relativeMasterChunkPath,
            Hasher hasher,
            ChunkValueEncoding converter
    ) throws IOException {
        Path absolutePath = Paths.get("").toAbsolutePath().resolve(relativeMasterChunkPath);
        byte[] rawChunk = Files.readAllBytes(absolutePath);

        if (rawChunk.length != (1 << 24) * elementSize)
            throw new IllegalArgumentException("Размер masterChunk некорректен: " + rawChunk.length);

        this.totalElements = rawChunk.length / elementSize;
        this.masterEntries = new Entry[totalElements];
        for (int i = 0; i < totalElements; i++) {
            int off = i * elementSize;
            byte[] raw = Arrays.copyOfRange(rawChunk, off, off + elementSize);
            masterEntries[i] = new Entry(raw);
        }

        this.hasher = hasher;
        this.converter = converter;
    }

    public void sortChunkToFile(String outputDirectoryPath, int chunkIndex) throws IOException {
        long t0 = System.currentTimeMillis();
        System.out.println("📦 Обработка чанка #" + chunkIndex);

        Entry[] chunkEntries = new Entry[totalElements];
        for (int i = 0; i < totalElements; i++) {
            chunkEntries[i] = new Entry(Arrays.copyOf(masterEntries[i].raw, elementSize));
        }

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < totalElements; i++) {
            if (i % 2_000_000 == 0) {
                System.out.println("  ⏳ Генерация [" + i + "/" + totalElements + "]");
            }
            byte[] global = combineWithPrefix(chunkEntries[i].raw, chunkIndex);
            String encoded = converter.convertToBaseString(global);
            chunkEntries[i].hash = hasher.getBinHash(encoded);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("✅ Хэши сгенерированы за " + (t2 - t1) / 1000.0 + " сек");

        System.out.println("🚀 Быстрая сортировка (QuickSort)...");
        long t3 = System.currentTimeMillis();
        quickSort(chunkEntries, 0, chunkEntries.length - 1);
        long t4 = System.currentTimeMillis();
        System.out.println("✅ Отсортировано за " + (t4 - t3) / 1000.0 + " сек");

        Path outPath = Paths.get(outputDirectoryPath, "chunk_" + chunkIndex + ".bin");
        Path folder = outPath.getParent();
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            System.out.println("📁 Папка создана: " + folder.toAbsolutePath());
        }

        System.out.println("💾 Сохранение в: " + outPath.toAbsolutePath());
        long t5 = System.currentTimeMillis();

        // ⚡ Ускоренная запись с буфером 16 МБ
        try (BufferedOutputStream os = new BufferedOutputStream(
                Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                16 * 1024 * 1024 // 16 МБ буфер
        )) {
            for (Entry e : chunkEntries) {
                os.write(e.raw);
            }
        }

        long t6 = System.currentTimeMillis();
        System.out.println("✅ Записано за " + (t6 - t5) / 1000.0 + " сек");

        long t7 = System.currentTimeMillis();
        System.out.println("🎉 Чанк #" + chunkIndex + " готов за " + (t7 - t0) / 1000.0 + " сек\n");
    }

    private byte[] combineWithPrefix(byte[] local, int chunkIndex) {
        byte[] prefix = new byte[] {
                (byte) (chunkIndex >>> 16),
                (byte) (chunkIndex >>> 8),
                (byte) chunkIndex
        };
        byte[] result = new byte[prefix.length + local.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(local, 0, result, prefix.length, local.length);
        return result;
    }

    private int compareHashes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int ai = a[i] & 0xFF;
            int bi = b[i] & 0xFF;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return Integer.compare(a.length, b.length);
    }

    private int compareRaw(byte[] a, byte[] b) {
        for (int i = 0; i < elementSize; i++) {
            int ai = a[i] & 0xFF;
            int bi = b[i] & 0xFF;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private void quickSort(Entry[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    private int partition(Entry[] arr, int low, int high) {
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

    private static class Entry {
        final byte[] raw;
        byte[] hash;
        Entry(byte[] raw) { this.raw = raw; }
    }
}