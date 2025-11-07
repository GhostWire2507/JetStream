package com.jetstream.application;

import com.jetstream.config.AppConfig;
import com.jetstream.database.DatabaseConnection;
import com.jetstream.utils.SceneManager;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main entry class for JetStream.
 * Contains a static showAlert utility (global) for consisten comalerts.
 */
public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        AppConfig.load(); // load config first
        DatabaseConnection.init(); // initialize DB connection (static)
        primaryStage = stage;
        SceneManager.getInstance().init(primaryStage);
        SceneManager.getInstance().loadMain(); // load main menu
        stage.setTitle("JetStream Airline");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        DatabaseConnection.close();
    }

    /**
     * Global UI alert helper
     */
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}
