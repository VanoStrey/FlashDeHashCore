package coreChunk;

import java.io.*;

public class ChunkBinaryFileAccessor implements AutoCloseable {
    private int ELEMENT_SIZE;

    private final byte[] globalOffset;
    private final int globalOffsetLen;
    private final long totalElements;
    private final RandomAccessFile raf;

    public ChunkBinaryFileAccessor(String filePath, Integer elementSize) throws IOException {
        ELEMENT_SIZE = elementSize;
        int chunkIndex = extractChunkIndex(filePath);
        this.globalOffset = encodeChunkOffset(chunkIndex);
        this.globalOffsetLen = globalOffset.length;

        File file = new File(filePath);
        this.raf = new RandomAccessFile(file, "r");
        long fileSize = raf.length();
        this.totalElements = fileSize / ELEMENT_SIZE;
    }

    public int getELEMENT_SIZE(){
        return ELEMENT_SIZE;
    }

    private int extractChunkIndex(String path) {
        String fileName = new File(path).getName();
        try {
            int start = fileName.indexOf("chunk_") + 6;
            int end = fileName.indexOf('.', start);
            return Integer.parseInt(fileName.substring(start, end));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось определить индекс чанка из имени файла: " + path, e
            );
        }
    }

    public byte[] getElement(long index) throws IOException {
        if (index < 0 || index >= totalElements) return null;

        long byteOffset = index * ELEMENT_SIZE;
        raf.seek(byteOffset);

        byte[] result = new byte[globalOffsetLen + ELEMENT_SIZE];

        // 1) Копируем глобальное смещение
        System.arraycopy(globalOffset, 0, result, 0, globalOffsetLen);

        // 2) Читаем 3 байта значения
        int read = raf.read(result, globalOffsetLen, ELEMENT_SIZE);
        if (read != ELEMENT_SIZE) return null;

        return result;
    }

    private byte[] encodeChunkOffset(int chunkIndex) {
        int shiftBits = 24;
        int totalBytes = (shiftBits + 7) / 8; // минимум 3 байта
        byte[] result = new byte[totalBytes];
        int local = chunkIndex;
        for (int i = totalBytes - 1; i >= 0; i--) {
            result[i] = (byte)(local & 0xFF);
            local >>>= 8;
        }
        return result;
    }

    public long getTotalElements() {
        return totalElements;
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
