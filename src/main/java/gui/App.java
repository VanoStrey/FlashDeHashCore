package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private MainController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Binary Tool for Cracking Hash");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);

        // Получаем контроллер для доступа к его методам
        controller = loader.getController();

        stage.setOnCloseRequest(event -> {
            controller.saveDictionaries(); // Сохраняем словари при закрытии
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
