package hashFunc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1Hash implements Hasher {
    private static final ThreadLocal<MessageDigest> DIG =
            ThreadLocal.withInitial(() -> {
                try { return MessageDigest.getInstance("SHA1"); }
                catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            });

    @Override
    public String getName() { return "SHA1"; }

    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        MessageDigest md = DIG.get();
        md.reset();
        for (int i = offset, end = offset + length; i < end; i++) {
            md.update((byte) chars[i]);
        }
        return md.digest();
    }

    @Override
    public byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len/2];
        for (int i = 0; i < len; i += 2) {
            out[i/2] = (byte)((Character.digit(hex.charAt(i),16)<<4)
                    + Character.digit(hex.charAt(i+1),16));
        }
        return out;
    }
}
