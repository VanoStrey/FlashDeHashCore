package hashFunc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class CRC32Hash implements Hasher {
    
    @Override
    public String getName() {
        return "CRC32";
    }
    
    @Override
    public byte[] getBinHash(char[] chars, int offset, int length) {
        // Конвертируем char[] в String, затем в UTF-8 байты
        String str = new String(chars, offset, length);
        return getBinHash(str);
    }
    
    @Override
    public byte[] getBinHash(String input) {
        CRC32 crc32 = new CRC32();
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        crc32.update(bytes);
        
        long value = crc32.getValue();
        
        // Конвертируем long в 4 байта (big-endian для CRC32)
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(value);
        byte[] allBytes = buffer.array();
        
        // Берем только последние 4 байта (CRC32 - 32 бита)
        byte[] result = new byte[4];
        System.arraycopy(allBytes, 4, result, 0, 4);
        
        return result;
    }
    
    @Override
    public byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }
    
    // Более эффективная версия для проверки CRC32 от байтов
    public long getCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
}