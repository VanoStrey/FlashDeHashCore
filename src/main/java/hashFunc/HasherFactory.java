package hashFunc;

public class HasherFactory {
    public static Hasher getHasher(String name) {
        return switch (name.toUpperCase()) {
            case "SHA256" -> new SHA256Hash();
            case "MD5" -> new MD5Hash();
            case "SHA1" -> new SHA1Hash();
            default -> throw new IllegalArgumentException("⛔ Неизвестный алгоритм хеширования: " + name);
        };
    }
}
