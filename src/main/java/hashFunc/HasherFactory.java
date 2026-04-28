package hashFunc;

public class HasherFactory {
    public static Hasher getHasher(String name) {
        return switch (name.toUpperCase()) {
            case "SHA256" -> new SHA256Hash();
            case "MD5" -> new MD5Hash();
            case "SHA1" -> new SHA1Hash();
            case "SHA3" -> new SHA3Hash();
            case "BLAKE3" -> new BLAKE3Hash();
            case "SHAKE128" -> new SHAKE128();
            case "CSHAKE128" -> new CSHAKE128();
            case "CRC32" -> new CRC32Hash();
            default -> throw new IllegalArgumentException("⛔ Неизвестный алгоритм хеширования: " + name);
        };
    }
}
