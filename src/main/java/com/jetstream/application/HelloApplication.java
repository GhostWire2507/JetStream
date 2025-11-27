package com.jetstream.application;

import com.jetstream.config.AppConfig;
import com.jetstream.database.DatabaseConnection;
import com.jetstream.utils.SceneManager;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main entry class for JetStream.
 * Contains a static showAlert utility (global) for consistent alerts.
 */
public class HelloApplication extends Application {

    private static final Logger logger = Logger.getLogger(HelloApplication.class.getName());
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            logger.info("JetStream Airline - Starting Application");

            logger.info("Step 1: Loading configuration...");
            AppConfig.load();
            logger.info("✓ Configuration loaded");

            logger.info("Step 2: Initializing database connection...");
            DatabaseConnection.init();
            logger.info("✓ Database connection initialized");

            logger.info("Step 3: Setting up primary stage...");
            primaryStage = stage;
            SceneManager.getInstance().init(primaryStage);
            logger.info("✓ Stage initialized");

            logger.info("Step 4: Loading login scene...");
            SceneManager.getInstance().loadLogin();
            logger.info("✓ Login scene loaded");

            logger.info("Step 5: Showing window...");
            stage.setTitle("JetStream Airline - Login");
            stage.setWidth(900);
            stage.setHeight(600);
            stage.show();
            logger.info("✓ Application window displayed");

            logger.info("Application Started Successfully!");

        } catch (Exception e) {
            logger.severe("CRITICAL ERROR DURING STARTUP: " + e.getMessage());
            e.printStackTrace();
            showAlert("Startup Error", "Failed to start application: " + e.getMessage());
            throw e;
        }
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
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            logger.severe("UNCAUGHT EXCEPTION IN THREAD: " + t.getName());
            e.printStackTrace();
        });
        launch();
    }
}
