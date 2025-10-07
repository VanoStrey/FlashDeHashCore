package coreChunk;

import java.math.BigInteger;

public class ChunkValueEncoding {
    private final String rangeChars;
    private final int base;
    private final ThreadLocal<char[]> TL_CHARS;

    public ChunkValueEncoding(String rangeChars) {
        this.rangeChars = rangeChars;
        this.base = rangeChars.length();
        this.TL_CHARS = ThreadLocal.withInitial(() -> new char[128]);
    }

    public String getRangeChars() {
        return rangeChars;
    }

    public int encodeToChars(byte[] value, char[] out) {
        BigInteger num = BigInteger.ZERO;
        for (byte b : value) {
            num = num.shiftLeft(8).or(BigInteger.valueOf(b & 0xFFL));
        }

        int pos = out.length;
        while (!num.equals(BigInteger.ZERO)) {
            BigInteger q = num.divide(BigInteger.valueOf(base));
            BigInteger r = num.subtract(q.multiply(BigInteger.valueOf(base)));
            out[--pos] = rangeChars.charAt(r.intValue());
            num = q;
        }

        if (pos == out.length) {
            out[--pos] = rangeChars.charAt(0);
        }
        return out.length - pos;
    }

    public String convertToBaseString(byte[] value) {
        int bufferSize = Math.max(value.length * 3, 128);
        char[] buf = new char[bufferSize];
        int len = encodeToChars(value, buf);
        return new String(buf, buf.length - len, len);
    }

    public byte[] decodeFromString(String encodedString) {
        BigInteger num = BigInteger.ZERO;
        for (char c : encodedString.toCharArray()) {
            int index = rangeChars.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Неверный символ в строке: " + c);
            }
            BigInteger baseBI = BigInteger.valueOf(base);
            BigInteger indexBI = BigInteger.valueOf(index);
            num = num.multiply(baseBI).add(indexBI);
        }

        int byteLength = (num.bitLength() + 7) / 8;
        if (num.equals(BigInteger.ZERO)) byteLength = 1;

        byte[] result = new byte[byteLength];
        BigInteger mask = BigInteger.valueOf(0xFF);
        for (int i = result.length - 1; i >= 0; i--) {
            BigInteger byteValue = BigInteger.valueOf(num.and(mask).intValue());
            result[i] = byteValue.byteValue();
            num = num.shiftRight(8);
        }
        return result;
    }
}
