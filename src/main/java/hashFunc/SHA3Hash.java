package hashFunc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA3Hash implements Hasher {
    @Override
    public String getName() { return "SHA3-256"; }

    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA3-256");
            md.update(new String(chars, offset, length).getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA3-256 algorithm not available", e);
        }
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