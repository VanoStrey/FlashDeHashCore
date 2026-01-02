package coreChunk;

import hashFunc.Hasher;

import java.io.IOException;

public class HashBinarySearch {
    private final ChunkBinaryFileAccessor accessor;
    private final ChunkValueEncoding converter;
    private final Hasher hasher;
    char[] buf = new char[32];

    public HashBinarySearch(ChunkBinaryFileAccessor accessor,
                            ChunkValueEncoding converter,
                            Hasher hasher) {
        this.accessor = accessor;
        this.converter = converter;
        this.hasher = hasher;
    }

    public String search(String targetHashHEX, long low, long high) throws IOException {
        byte[] target = hasher.hexToBytes(targetHashHEX);

        while (low <= high) {
            long mid = (low + high) >>> 1;
            byte[] elem = accessor.getElement(mid);
            int len = converter.encodeToChars(elem, buf);
            byte[] hash = hasher.getBinHash(buf, buf.length - len, len);

            int cmp = compareHashes(hash, target);
            if (cmp < 0)      low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return new String(buf, buf.length - len, len);
        }
        return "";
    }

    private int compareHashes(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            int diff = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (diff != 0) return diff;
        }
        return a.length - b.length;
    }
}