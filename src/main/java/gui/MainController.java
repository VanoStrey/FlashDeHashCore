package gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML
    private TableView<String> dictionariesTable;
    @FXML
    private Button searchInDictBtn;

    private final ObservableList<String> dictionaries = FXCollections.observableArrayList();
    private final DictionaryStorage storage = new DictionaryStorage();

    @FXML
    public void initialize() {
        loadDictionaries();

        TableColumn<String, String> nameCol = new TableColumn<>("Название словаря");
        nameCol.setCellValueFactory(cellData -> {
            String path = cellData.getValue();
            return new SimpleStringProperty(getDictionaryName(path));
        });

        TableColumn<String, String> pathCol = new TableColumn<>("Путь");
        pathCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()));

        dictionariesTable.getColumns().addAll(nameCol, pathCol);
        dictionariesTable.setItems(dictionaries);
        dictionariesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }


    // Загружает пути к словарям из файла при старте приложения
    private void loadDictionaries() {
        try {
            List<String> loaded = storage.loadDictionaries();
            dictionaries.setAll(loaded);
        } catch (IOException e) {
            showAlert("Ошибка загрузки", "Не удалось загрузить словари: " + e.getMessage());
        }
    }

    // Сохраняет пути к словарям в файл перед закрытием
    public void saveDictionaries() {
        try {
            storage.saveDictionaries(dictionaries);
        } catch (IOException e) {
            showAlert("Ошибка сохранения", "Не удалось сохранить словари: " + e.getMessage());
        }
    }

    @FXML
    private void addDictionary() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку со словарём");

        Window stage = dictionariesTable.getScene().getWindow();
        File selectedDir = chooser.showDialog(stage);

        if (selectedDir != null && selectedDir.isDirectory()) {
            String path = selectedDir.getAbsolutePath();
            dictionaries.add(path);
            saveDictionaries(); // Сохраняем сразу после добавления
        } else if (selectedDir != null) {
            showAlert("Ошибка", "Выбранная директория не существует или не является папкой.");
        }
    }

    @FXML
    private void removeDictionary() {
        String selected = dictionariesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            dictionaries.remove(selected);
            saveDictionaries(); // Сохраняем после удаления
        } else {
            showAlert("Ошибка", "Выберите словарь для удаления");
        }
    }

    @FXML
    private void refreshDictionaries() {
        dictionariesTable.refresh();
    }

    @FXML
    private void openSearchWindow() {
        String selected = dictionariesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/search.fxml"));
                Stage stage = new Stage();
                Scene scene = new Scene(loader.load());
                stage.setScene(scene);

                SearchController searchController = loader.getController();
                searchController.setDictionaryPath(selected);

                stage.setTitle("Поиск в словаре: " + getDictionaryName(selected));
                stage.setMinWidth(500);
                stage.setMinHeight(400);
                stage.show();
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось открыть окно поиска: " + e.getMessage());
            }
        } else {
            showAlert("Ошибка", "Выберите словарь для поиска");
        }
    }



    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getDictionaryName(String path) {
        File file = new File(path);
        return file.getName(); // Возвращает только имя папки, без пути
    }
}
