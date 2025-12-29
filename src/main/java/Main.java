import benchmark.BatchPerformanceMonitor;
import coreChunk.*;
import hashFunc.*;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.net.ssl.SNIHostName;
import javax.sound.midi.Soundbank;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class Main {

    public static ArrayList<HashBinarySearch> hashBinarySearch = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        /*
        System.out.print("\nКонсоль или Телеграмм бот?(0/1): ");
        Scanner scanner = new Scanner(System.in);
        switch (Integer.parseInt(scanner.nextLine())){
            case 0:
                menuApp(new DictionarySearch(selectDictionaryFolder(), true));
                break;
            case 1:
                startTelegramBot(new DictionarySearch(selectDictionaryFolder(), true));
                break;
        }
        /**/

        String outputDir = selectDictionaryFolder();
        Hasher hasher = new CSHAKE128();
        String dictionarySymbols = "0123456789";

        ChunkValueEncoding chunkValueEncoding = new ChunkValueEncoding(dictionarySymbols);
        HashSortedChunkBuilder builder = new HashSortedChunkBuilder("master_chunk.bin", hasher, chunkValueEncoding);
        for (int i = getMaxChunkIndex(outputDir) + 1; i < 60; i++) {
            builder.sortChunkToFile(outputDir, i);
            int maxChunkIndex = getMaxChunkIndex(outputDir);
            ChunkBinaryFileAccessor chunkBinaryFileAccessor = new ChunkBinaryFileAccessor(outputDir +"/chunk_" + maxChunkIndex + ".bin", 3);
            System.out.println("Последняя комбинация в чанке " + maxChunkIndex + " : " +
                    chunkValueEncoding.convertToBaseString(chunkBinaryFileAccessor.getElement(chunkBinaryFileAccessor.getTotalElements()-1)));
            System.out.println("Максимальная комбинация в чанке " + maxChunkIndex + " : " +
                    chunkValueEncoding.convertToBaseString(getMaxCombinationForChunk(maxChunkIndex)) + "\n\n");
        }
        startTelegramBot(new DictionarySearch(outputDir, true));
        /*
        */
    }

    private static int compareRaw(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            int ai = a[i] & 0xFF;
            int bi = b[i] & 0xFF;
            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }


    public static String selectDictionaryFolder() {
        File currentDir = new File(".");
        File[] directories = currentDir.listFiles(File::isDirectory);

        if (directories == null || directories.length == 0) {
            System.out.println("❌ В текущей папке не найдено ни одной директории.");
            return null;
        }

        System.out.println("📁 Доступные словари:");
        for (int i = 0; i < directories.length; i++) {
            System.out.println("[" + i + "] " + directories[i].getName());
        }

        System.out.print("\n🔢 Введите номер нужной папки: ");
        Scanner scanner = new Scanner(System.in);
        int choice;

        while (true) {
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 0 && choice < directories.length) break;
                System.out.print("❗ Неверный номер, попробуйте ещё раз: ");
            } catch (NumberFormatException e) {
                System.out.print("❗ Введите корректный номер: ");
            }
        }

        String selected = directories[choice].getName();
        System.out.println("✅ Вы выбрали: " + selected);
        return selected;
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
        double startTime, totalTime;
        while (true) {
            System.out.println("hash для расшифровки: ");
            hash = scanner.next();

            startTime = System.nanoTime();
            result =  dictionarySearch.search(hash);
            totalTime = System.nanoTime() - startTime;


            System.out.println("Результат: " + result);
            System.out.println("Время выполнения: " + String.format("%.2f", totalTime / 1_000_000.0) + " ms\n");
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