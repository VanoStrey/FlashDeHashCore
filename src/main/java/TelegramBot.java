import coreChunk.DictionarySearch;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {
    private static final String BOT_USERNAME = "";
    private static final String BOT_TOKEN = "";
    private static DictionarySearch dictionarySearch;

    public TelegramBot(DictionarySearch dictionarySearch){
        this.dictionarySearch = dictionarySearch;
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
                String status = getStatusMessage();
                sendResponse(chatId, status);
                return;
            }

            if (messageText.equalsIgnoreCase("/start")) {
                sendResponse(chatId,
                        "❗ Made by @VanoStrey ❗\n\n" +
                                "This is a prototype program. Right now, the set of characters in combinations is limited, " +
                                "but the algorithm supports any custom alphabet and hash function.\n\n" +
                                "The project demonstrates the power of instant hash cracking using a precomputed binary dictionary.");

                sendResponse(chatId,
                        "👋 Hi! Send me the hash — and I'll crack it right away 😈😈😈\n\n" +
                                "Supported algorithm: SHA-256\n\n" +
                                "Dictionary alphabet:  [A-Z][a-z][0-9]\n\n" +
                                "The dictionary includes *all* combinations from 1 to 6 characters.\n" +
                                "That's a total of 57,731,386,986 unique combinations.\n\n" +
                                "Ready when you are 🔥");

                return;
            } else {
                long startTime = System.currentTimeMillis();
                String result = null;
                try {
                    result = dictionarySearch.search(messageText);  // Получаем результат поиска
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long endTime = System.currentTimeMillis();

                // Отправляем три отдельных сообщения
                sendResponse(chatId, result);  // Результат хеша
                sendResponse(chatId, "⏳ Time : " + (endTime - startTime) + " ms");  // Время поиска
            }
        }
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

        return "📊 *System Status*\n"
                + "💽 OS: " + os + "\n"
                + "🧠 Max RAM for JVM: " + totalMemoryMB + " MB\n"
                + "🧵 CPU Cores: " + availableProcessors + "\n"
                + "📦 Storage used: 187.6 GB (NVMe)\n";
    }

}
