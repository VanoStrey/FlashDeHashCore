package gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import coreChunk.ChunkValueEncoding;
import coreChunk.HashSortedChunkBuilder;
import hashFunc.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CreateDictionaryController {

    @FXML
    private TextField masterChunkField, outputDirField, chunkCountField, symbolsField;
    @FXML
    private ComboBox<String> hasherCombo;
    @FXML
    private TextArea statusArea;

    private Stage stage;
    private MainController mainController; // для добавления словаря в главный список


    // Устанавливаем Stage и MainController
    public void setStage(Stage stage) { this.stage = stage; }
    public void setMainController(MainController mainController) { this.mainController = mainController; }

    private void createMetadataFile(String outputDirPath) throws IOException {
        // Собираем данные
        String symbols = " " + symbolsField.getText().trim();
        String hashAlgo = hasherCombo.getValue(); // уже проверено (SHA256/MD5)

        // Формируем JSON
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("dictionary_symbols", symbols);
        metadata.put("hash_algorithm", hashAlgo);
        metadata.put("element_size", "3");

        // Используем Jackson для сериализации (если есть в зависимостях)
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // красивый вывод

        mapper.writeValue(new File(outputDirPath, "metadata.json"), metadata);

    }

    @FXML
    private void browseMasterChunk() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите masterChunk (48 МБ)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Binary files", "*.bin"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) masterChunkField.setText(file.getAbsolutePath());
    }

    @FXML
    private void browseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку для сохранения словаря");
        File dir = chooser.showDialog(stage);
        if (dir != null) outputDirField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void startCreation() {
        if (!validateInputs()) return;

        String masterChunkPath = masterChunkField.getText();
        String outputDirPath = outputDirField.getText();
        int chunkCount = Integer.parseInt(chunkCountField.getText());
        Hasher hasher = HasherFactory.getHasher(hasherCombo.getValue());


        try {
            // 1. Создаём metadata.json
            createMetadataFile(outputDirPath);
            appendStatus("✅ metadata.json создан\n");

            // 2. Генерируем чанки
            HashSortedChunkBuilder builder = new HashSortedChunkBuilder(
                    masterChunkPath, hasher, new ChunkValueEncoding( " " + symbolsField.getText().trim())
            );

            for (int i = 0; i < chunkCount; i++) {
                appendStatus("Обработка чанка #" + i + "...\n");
                builder.sortChunkToFile(outputDirPath, i);
            }

            appendStatus("✅ Словарь создан успешно!\n");

            if (mainController != null) {
                String folderName = new File(outputDirPath).getName();
                mainController.dictionaries.add(outputDirPath);
                mainController.saveDictionaries();
            }

        } catch (Exception e) {
            appendStatus("❌ Ошибка: " + e.getMessage() + "\n");
        }
    }


    @FXML
    private void cancel() { stage.close(); }

    // Валидация входных данных
    private boolean validateInputs() {
        if (masterChunkField.getText().isEmpty()) {
            appendStatus("Ошибка: не выбран masterChunk\n");
            return false;
        }
        if (outputDirField.getText().isEmpty()) {
            appendStatus("Ошибка: не выбрана папка вывода\n");
            return false;
        }
        try {
            int count = Integer.parseInt(chunkCountField.getText());
            if (count <= 0 || count > 1024) {
                appendStatus("Ошибка: количество чанков должно быть от 1 до 1024\n");
                return false;
            }
        } catch (NumberFormatException e) {
            appendStatus("Ошибка: некорректное количество чанков\n");
            return false;
        }
        return true;
    }

    // Добавляет текст в область статуса
    private void appendStatus(String text) {
        statusArea.appendText(text);
    }
}
