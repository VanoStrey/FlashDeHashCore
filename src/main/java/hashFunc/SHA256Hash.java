package hashFunc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256Hash implements Hasher {
    private static final ThreadLocal<MessageDigest> DIG =
            ThreadLocal.withInitial(() -> {
                try { return MessageDigest.getInstance("SHA-256"); }
                catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            });

    @Override
    public String getName() { return "SHA256"; }

    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        MessageDigest md = DIG.get();
        md.reset();
        // Преобразуем весь массив chars в один вызов update
        md.update(new String(chars, offset, length).getBytes());
        return md.digest();
    }

    @Override
    public byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hex.charAt(i), 16) << 4;
            int low = Character.digit(hex.charAt(i + 1), 16);
            out[i / 2] = (byte) (high + low);
        }
        return out;
    }
}
