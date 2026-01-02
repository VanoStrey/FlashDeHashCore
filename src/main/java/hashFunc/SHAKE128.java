package hashFunc;

import org.bouncycastle.crypto.digests.SHAKEDigest;
import java.util.HexFormat;

public class SHAKE128 implements Hasher {
    private final int outputLength; // в битах
    
    public SHAKE128() { this(256); }
    public SHAKE128(int outputBits) { 
        this.outputLength = outputBits; 
    }
    
    @Override
    public String getName() { 
        return "SHAKE128";
    }
    
    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        SHAKEDigest digest = new SHAKEDigest(128);
        
        // Конвертируем char[] в byte[]
        for (int i = offset; i < offset + length; i++) {
            char c = chars[i];
            // Простая конвертация (подходит для ASCII 0-9)
            digest.update((byte) c);
        }
        
        byte[] output = new byte[outputLength / 8];
        digest.doFinal(output, 0, output.length);
        return output;
    }
    
    @Override
    public byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}