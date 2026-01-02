package gui;

import coreChunk.DictionarySearch;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class SearchController {
    private String dictionaryName;
    private DictionarySearch dictionarySearch;

    public void setDictionaryPath(String dictionaryPath) throws IOException {
        dictionarySearch = new DictionarySearch(dictionaryPath, true);
        dictionaryName = new File(dictionaryPath).getName();
    }

    @FXML
    private TextField hashField;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea resultArea;
    @FXML
    private Button clearButton;
    @FXML
    private Button closeButton;

    @FXML
    private void onSearch() throws InterruptedException {
        String hash = hashField.getText();
        if (hash == null || hash.trim().isEmpty()) {
            showAlert("Предупреждение", "Введите хеш для поиска");
            return;
        }

        progressBar.setVisible(true);
        resultArea.setText(dictionarySearch.search(hash));
        progressBar.setVisible(false);
    }

    @FXML
    private void onClear() {
        hashField.clear();
        resultArea.clear();
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
