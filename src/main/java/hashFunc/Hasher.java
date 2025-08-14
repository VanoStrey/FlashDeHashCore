package hashFunc;

public interface Hasher {
    String getName();

    /** Основной метод – хеширует символы chars[offset..offset+length). */
    byte[] getBinHash(char[] chars, int offset, int length);

    /** Обратная совместимость для строк */
    default byte[] getBinHash(String input) {
        char[] arr = input.toCharArray();
        return getBinHash(arr, 0, arr.length);
    }

    /** HEX → байты */
    byte[] hexToBytes(String hex);

    /** HEX-строка из байтов */
    default String getHash(String input) {
        byte[] bin = getBinHash(input);
        StringBuilder sb = new StringBuilder(bin.length * 2);
        for (byte b : bin) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    default String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
