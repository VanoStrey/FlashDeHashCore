package coreChunk;

import java.math.BigInteger;

public class ChunkValueEncoding {
    private final String rangeChars;
    private final int base;
    private int maxDigitsPerByte;
    private final boolean baseIsPowerOfTwo;
    private final int baseLog2;
    private final BigInteger baseBI;

    public ChunkValueEncoding(String rangeChars) {
        this.rangeChars = rangeChars;
        this.base = rangeChars.length();
        this.baseBI = BigInteger.valueOf(base);

        // Предвычисляем максимальное количество цифр на байт
        maxDigitsPerByte = (int) Math.ceil(Math.log(256) / Math.log(base));
        if (maxDigitsPerByte < 1) maxDigitsPerByte = 1;

        this.baseIsPowerOfTwo = (base & (base - 1)) == 0 && base > 0;
        this.baseLog2 = baseIsPowerOfTwo ? Integer.numberOfTrailingZeros(base) : -1;
    }

    public String getRangeChars() {
        return rangeChars;
    }

    public int encodeToChars(byte[] value, char[] out) {
        // Для 6 байт используем быструю оптимизацию
        if (value.length == 6) {
            return encodeToChars6Bytes(value, out);
        }

        // Fallback для других размеров
        BigInteger num = new BigInteger(1, value);
        int pos = out.length;

        while (!num.equals(BigInteger.ZERO)) {
            BigInteger[] divRem = num.divideAndRemainder(baseBI);
            out[--pos] = rangeChars.charAt(divRem[1].intValue());
            num = divRem[0];
        }

        if (pos == out.length) {
            out[--pos] = rangeChars.charAt(0);
        }
        return out.length - pos;
    }

    private int encodeToChars6Bytes(byte[] value, char[] out) {
        // Преобразуем 6 байтов в long (48 бит)
        long num = ((value[0] & 0xFFL) << 40) |
                ((value[1] & 0xFFL) << 32) |
                ((value[2] & 0xFFL) << 24) |
                ((value[3] & 0xFFL) << 16) |
                ((value[4] & 0xFFL) << 8) |
                (value[5] & 0xFFL);

        int pos = out.length;

        // Обработка нуля
        if (num == 0) {
            out[--pos] = rangeChars.charAt(0);
            return 1;
        }

        // Оптимизация для степеней двойки
        if (baseIsPowerOfTwo) {
            int shift = baseLog2;
            long mask = base - 1;

            do {
                out[--pos] = rangeChars.charAt((int)(num & mask));
                num >>>= shift;
            } while (num != 0);
        }
        // Общий случай
        else {
            do {
                long quotient = num / base;
                int remainder = (int)(num - quotient * base);
                out[--pos] = rangeChars.charAt(remainder);
                num = quotient;
            } while (num != 0);
        }

        return out.length - pos;
    }

    public String convertToBaseString(byte[] value) {
        // Точный расчет размера буфера
        char[] buf = new char[value.length * maxDigitsPerByte];
        int len = encodeToChars(value, buf);
        return new String(buf, buf.length - len, len);
    }

    public byte[] decodeFromString(String encodedString) {
        BigInteger num = BigInteger.ZERO;

        // Оптимизация для длинных строк
        for (int i = 0; i < encodedString.length(); i++) {
            char c = encodedString.charAt(i);
            int index = rangeChars.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Неверный символ в строке: " + c);
            }
            num = num.multiply(baseBI).add(BigInteger.valueOf(index));
        }

        int byteLength = (num.bitLength() + 7) / 8;
        if (num.equals(BigInteger.ZERO)) byteLength = 1;

        byte[] result = new byte[byteLength];
        byte[] bigIntBytes = num.toByteArray();

        // BigInteger может вернуть массив с ведущим нулевым байтом для знака
        int start = (bigIntBytes[0] == 0) ? 1 : 0;
        int len = bigIntBytes.length - start;

        System.arraycopy(bigIntBytes, start, result, result.length - len, len);
        return result;
    }
}