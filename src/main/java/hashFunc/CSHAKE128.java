package hashFunc;

import java.util.HexFormat;

// cSHAKE128 - customizable SHAKE (NIST SP 800-185)
public class CSHAKE128 implements Hasher {
    private final int outputLength;
    private final byte[] functionName;
    private final byte[] customization;
    
    public CSHAKE128() {
        this(256, new byte[0], new byte[0]);
    }
    
    public CSHAKE128(int outputBits, String functionName, String customization) {
        this(outputBits, 
             functionName != null ? functionName.getBytes() : new byte[0],
             customization != null ? customization.getBytes() : new byte[0]);
    }
    
    private CSHAKE128(int outputBits, byte[] fn, byte[] custom) {
        this.outputLength = outputBits;
        this.functionName = fn;
        this.customization = custom;
    }
    
    @Override
    public String getName() { 
        return "CSHAKE128";
    }
    
    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        // Упрощённая реализация cSHAKE
        // На практике используйте библиотеку типа BouncyCastle
        String input = new String(chars, offset, length);
        String combined = (functionName.length > 0 ? new String(functionName) : "") +
                         (customization.length > 0 ? new String(customization) : "") +
                         input;
        
        // Временная заглушка - используем SHA3-256
        try {
            java.security.MessageDigest md = 
                java.security.MessageDigest.getInstance("SHA3-256");
            return md.digest(combined.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("cSHAKE128 error", e);
        }
    }
    
    @Override
    public byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}