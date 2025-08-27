package coreChunk;

public class ChunkValueEncoding {
    private final String rangeChars;
    private final int base;
    private final ThreadLocal<char[]> TL_CHARS;

    public ChunkValueEncoding(String rangeChars) {
        this.rangeChars = rangeChars;
        this.base = rangeChars.length();
        // Размер буфера: достаточно 16 символов для 48-битного числа в основании ≤94
        this.TL_CHARS = ThreadLocal.withInitial(() -> new char[16]);
    }

    public String getRangeChars() {
        return rangeChars;
    }

    /**
     * Преобразует value (глобальный офсет + локальное значение) в символы,
     * записывая их в out справа налево.
     * Возвращает длину полезных символов в out.
     */
    public int encodeToChars(byte[] value, char[] out) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Недопустимое значение");
        }

        // Собираем 48-битное число из байтов
        long num = 0L;
        for (byte b : value) {
            num = (num << 8) | (b & 0xFFL);
        }

        int pos = out.length;
        // Делим аппаратно, пишем остаток
        while (num != 0L) {
            long q = num / base;
            int r  = (int)(num - q * base);
            out[--pos] = rangeChars.charAt(r);
            num = q;
        }

        // Если число было нулём — один символ «0»
        if (pos == out.length) {
            out[--pos] = rangeChars.charAt(0);
        }

        return out.length - pos;
    }

    public String convertToBaseString(byte[] value) {
        char[] buf = TL_CHARS.get();
        int len = encodeToChars(value, buf);
        return new String(buf, buf.length - len, len);
    }
}
