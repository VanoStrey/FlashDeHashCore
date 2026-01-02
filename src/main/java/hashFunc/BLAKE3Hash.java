package hashFunc;

import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.jcajce.provider.digest.Blake3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BLAKE3Hash implements Hasher {
    // Вариант 1: Используя BouncyCastle напрямую (рекомендуется для производительности)
    private static final ThreadLocal<Blake3Digest> DIG_DIRECT =
            ThreadLocal.withInitial(() -> new Blake3Digest(256));
    
    // Вариант 2: Через MessageDigest API (если нужна совместимость)
    private static final ThreadLocal<MessageDigest> DIG_MD =
            ThreadLocal.withInitial(() -> {
                try { 
                    // Требуется регистрация BouncyCastle как провайдера
                    java.security.Security.addProvider(
                        new org.bouncycastle.jce.provider.BouncyCastleProvider()
                    );
                    return MessageDigest.getInstance("BLAKE3-256", "BC"); 
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

    @Override
    public String getName() { return "BLAKE3"; }

    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        // Используем прямой вызов (более эффективно)
        Blake3Digest digest = DIG_DIRECT.get();
        digest.reset();
        
        // Преобразуем char[] в byte[]
        String str = new String(chars, offset, length);
        byte[] inputBytes = str.getBytes();
        
        digest.update(inputBytes, 0, inputBytes.length);
        
        byte[] output = new byte[32]; // 256 бит = 32 байта
        digest.doFinal(output, 0);
        return output;
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