import hashFunc.*;
import coreChunk.*;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class Main {

    public static ArrayList<HashBinarySearch> hashBinarySearch = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String outputDir = "chuncks_SHA256_[A-Z][a-z][0-9]";

        startTelegramBot(new DictionarySearch(outputDir));


        /*
        SHA256Hash sha256 = new SHA256Hash();
        String dictionarySymbols = " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        ChunkValueEncoding chunkValueEncoding = new ChunkValueEncoding(dictionarySymbols);

        HashSortedChunkBuilder builder = new HashSortedChunkBuilder("master_chunk.bin", sha256, chunkValueEncoding);
        for (int i = getMaxChunkIndex(outputDir) + 1; i < 3727; i++) {
            int maxChunkIndex = getMaxChunkIndex(outputDir);
            ChunkBinaryFileAccessor chunkBinaryFileAccessor = new ChunkBinaryFileAccessor(outputDir+"/chunk_"+ maxChunkIndex + ".bin");
            System.out.println("Последняя комбинация в чанке " + maxChunkIndex + " : " +
                    chunkValueEncoding.convertToBaseString(chunkBinaryFileAccessor.getElement(chunkBinaryFileAccessor.getTotalElements()-1)));
            System.out.println("Максимальная комбинация в чанке " + maxChunkIndex + " : " +
                    chunkValueEncoding.convertToBaseString(getMaxCombinationForChunk(maxChunkIndex)) + "\n\n");
            builder.sortChunkToFile(outputDir, i);
        }

        */
    }


    private static void startTelegramBot(DictionarySearch dictionarySearch) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot(dictionarySearch));
            System.out.println("🚀 TelegramBot успешно запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static void menuApp(DictionarySearch dictionarySearch) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);
        String hash, result;
        long startTime, endTime;
        while (true) {
            System.out.println("hash для расшифровки: ");
            hash = scanner.next();

            startTime = System.currentTimeMillis();
            result =  dictionarySearch.search(hash);
            endTime = System.currentTimeMillis();


            System.out.println("Результат: " + result);
            System.out.println("Время выполнения: " + (endTime - startTime) + " милисекунд\n");
        }
    }
    public static String getParentDirectory() {
        return new File(".").getAbsoluteFile().getParent();
    }

    public static String inputFolder(List<String> folders) {
        System.out.println("Доступные папки:");
        for (int i = 0; i < folders.size(); i++) {
            System.out.println((i + 1) + "... " + folders.get(i));
        }

        System.out.print("Выберите номер папки: ");
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt() - 1;

        return (choice >= 0 && choice < folders.size()) ? folders.get(choice) : "";
    }

    public static List<String> listAllFolders(String directoryPath) {
        File dir = new File(directoryPath);
        return Arrays.stream(dir.listFiles(File::isDirectory))
                .map(File::getName)
                .collect(Collectors.toList());
    }



    public static int getMaxChunkIndex(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Папка не существует: " + folder.getAbsolutePath());
        }

        int maxIndex = -1;
        for (String name : folder.list()) {
            if (name.startsWith("chunk_") && name.endsWith(".bin")) {
                try {
                    String numberPart = name.substring(6, name.length() - 4);
                    int index = Integer.parseInt(numberPart);
                    if (index > maxIndex) {
                        maxIndex = index;
                    }
                } catch (NumberFormatException ignored) {
                    // Пропускаем, если имя невалидно
                }
            }
        }

        return maxIndex;
    }

    public static byte[] getMaxCombinationForChunk(int chunkIndex) {
        byte[] result = new byte[6];
        result[0] = (byte) ((chunkIndex >> 16) & 0xFF);
        result[1] = (byte) ((chunkIndex >> 8) & 0xFF);
        result[2] = (byte) (chunkIndex & 0xFF);
        result[3] = (byte) 0xFF;
        result[4] = (byte) 0xFF;
        result[5] = (byte) 0xFF;
        return result;
    }

}