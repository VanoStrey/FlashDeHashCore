package coreChunk;

import hashFunc.Hasher;

import java.io.IOException;

public class HashBinarySearch {
    private final ChunkBinaryFileAccessor accessor;
    private final ChunkValueEncoding converter;
    private final Hasher hasher;
    long total;
    char[] buf = new char[32];
    byte[] lowHash;
    byte[] highHash;
    private final long RANGE;

    public HashBinarySearch(ChunkBinaryFileAccessor accessor,
                            ChunkValueEncoding converter,
                            Hasher hasher) throws IOException {
        this.accessor = accessor;
        this.converter = converter;
        this.hasher = hasher;

        RANGE = accessor.getELEMENT_SIZE() == 3 ? 8192 : 65536;

        total = accessor.getTotalElements();
        lowHash = getHash(0, buf);
        highHash = getHash(total - 1, buf);
    }

    public String search(String targetHashHEX) throws IOException {
        if (total == 0) return "";

        byte[] target = hasher.hexToBytes(targetHashHEX);

        long predicted = predictIndex(0, total - 1, target, buf, accessor.getELEMENT_SIZE());
        long low  = Math.max(0, predicted - RANGE);
        long high = Math.min(total - 1, predicted + RANGE);

        while (low <= high) {
            long mid = (low + high) >>> 1;
            byte[] elem = accessor.getElement(mid);
            int len = converter.encodeToChars(elem, buf);
            byte[] hash = hasher.getBinHash(buf, buf.length - len, len);

            int cmp = compareHashes(hash, target);
            if (cmp < 0)      low  = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return new String(buf, buf.length - len, len);
        }
        return "";
    }

    private long predictIndex(long low, long high, byte[] target, char[] buf, int bytes) throws IOException {
        long lv = extractBytes(lowHash, bytes);
        long hv = extractBytes(highHash, bytes);
        long tv = extractBytes(target, bytes);

        if (hv == lv) return low;
        return low + (long)((double)(tv - lv) * (high - low) / (hv - lv));
    }

    private byte[] getHash(long index, char[] buf) throws IOException {
        int len = converter.encodeToChars(accessor.getElement(index), buf);
        return hasher.getBinHash(buf, buf.length - len, len);
    }

    private long extractBytes(byte[] data, int bytes) {
        long val = 0;
        for (int i = 0; i < bytes; i++) val = (val << 8) | (data[i] & 0xFF);
        return val;
    }

    private int compareHashes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int diff = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (diff != 0) return diff;
        }
        return a.length - b.length;
    }
}
