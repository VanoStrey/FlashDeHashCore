import coreChunk.DictionarySearch;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.json.JSONObject;

import java.io.*;

public class TelegramBot extends TelegramLongPollingBot {
    private static String BOT_USERNAME;
    private static String BOT_TOKEN;
    private static DictionarySearch dictionarySearch;

    public TelegramBot(DictionarySearch dictionarySearch) {
        TelegramBot.dictionarySearch = dictionarySearch;
        loadOrCreateBotConfig();
    }

    private void loadOrCreateBotConfig() {
        File configFile = new File("bot_config.json");

        if (!configFile.exists()) {
            System.out.println("❗ Конфигурационный файл bot_config.json не найден.");
            System.out.println("📄 Создаю шаблон...");

            JSONObject template = new JSONObject();
            template.put("token", "ВСТАВЬ_СЮДА_ТОКЕН");
            template.put("username", "ВСТАВЬ_СЮДА_ИМЯ_БОТА");

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(template.toString(4));
                System.out.println("✅ Файл bot_config.json создан. Пожалуйста, открой его и вставь данные.");
            } catch (IOException e) {
                System.err.println("❌ Ошибка при создании bot_config.json: " + e.getMessage());
            }

            System.exit(1);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONObject json = new JSONObject(jsonContent.toString());
            BOT_TOKEN = json.getString("token");
            BOT_USERNAME = json.getString("username");

            if (BOT_TOKEN.contains("ВСТАВЬ") || BOT_USERNAME.contains("ВСТАВЬ")) {
                System.out.println("⚠️ Пожалуйста, заполните bot_config.json перед запуском бота.");
                System.exit(1);
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка при чтении bot_config.json: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String messageText = update.getMessage().getText();

            if (messageText.equalsIgnoreCase("/status")) {
                sendResponse(chatId, getStatusMessage());
                return;
            }

            if (messageText.equalsIgnoreCase("/start")) {
                sendResponse(chatId,
                        "❗ Сделал @VanoStrey ❗\n\n" +
                                "Это прототип программы. Пока что набор символов ограничен, " +
                                "но алгоритм поддерживает любые алфавиты и хеш-функции.\n\n" +
                                "Проект демонстрирует возможности моментального подбора хеша с использованием бинарного словаря.");

                sendResponse(chatId,
                        "👋 Привет! Отправь мне хеш — и я моментально его взломаю 😈😈😈\n\n" +
                                "Поддерживаемый алгоритм: " + dictionarySearch.hasher.getName() + "\n\n" +
                                "Алфавит словаря:\n\n" + dictionarySearch.converter.getRangeChars() + "\n\n" +
                                "Всего " + formatNumber((long) (dictionarySearch.hashBinarySearch.size() * Math.pow(2, 24))) + " уникальных комбинаций.\n\n" +
                                "Готов к работе 🔥");
                return;
            }

            long startTime = nowNanos();
            String result;
            try {
                result = dictionarySearch.search(messageText);
            } catch (InterruptedException e) {
                result = "❌ Ошибка: " + e.getMessage();
            }
            long endTime = nowNanos();

            sendResponse(chatId, result);
            String elapsedTime = formatElapsedTime(endTime - startTime);
            sendResponse(chatId, "⏳ Время подбора: " + elapsedTime);
        }
    }

    public static String formatNumber(long number) {
        String numberStr = String.valueOf(number);
        StringBuilder result = new StringBuilder();

        for (int i = numberStr.length() - 1, count = 0; i >= 0; i--) {
            result.insert(0, numberStr.charAt(i));
            count++;
            if (count % 3 == 0 && i != 0) {
                result.insert(0, ".");
            }
        }
        return result.toString();
    }

    private void sendResponse(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getStatusMessage() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long totalMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String os = System.getProperty("os.name");

        return "📊 *Системный статус*\n" +
                "💽 Операционная система: " + os + "\n" +
                "🧠 Память для JVM: " + totalMemoryMB + " МБ\n" +
                "🧵 Ядер процессора: " + availableProcessors + "\n" +
                "📦 Размер словаря: 187.6 ГБ (NVMe)";
    }

    /**
     * Универсальный таймер: на Android берём SystemClock.elapsedRealtimeNanos(),
     * на обычной JVM — System.nanoTime().
     */
    private static long nowNanos() {
        try {
            Class<?> sysClock = Class.forName("android.os.SystemClock");
            return (long) sysClock.getMethod("elapsedRealtimeNanos").invoke(null);
        } catch (Exception e) {
            return System.nanoTime();
        }
    }

    /**
     * Форматирует время:
     * - для ПК: до 2 знаков после запятой (мс)
     * - для Android: целое значение (мс)
     */
    private static String formatElapsedTime(long nanos) {
        double elapsedMs = nanos / 1_000_000.0;

        // Проверка, что мы на Android
        if (isAndroid()) {
            return String.format("%d мс", Math.round(elapsedMs)); // округляем до целых
        } else {
            return String.format("%.2f мс", elapsedMs); // точность до 2 знаков
        }
    }

    private static boolean isAndroid() {
        try {
            // Если это Android, то класс android.os.SystemClock будет найден
            Class.forName("android.os.SystemClock");
            return true;
        } catch (ClassNotFoundException e) {
            return false; // Не Android
        }
    }
}
