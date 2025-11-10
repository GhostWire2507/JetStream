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
        try {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║          JetStream Airline - Starting Application         ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");

            System.out.println("Step 1: Loading configuration...");
            AppConfig.load(); // load config first
            System.out.println("✓ Configuration loaded\n");

            System.out.println("Step 2: Initializing database connections...");
            DatabaseConnection.init(); // initialize DB connection (static)
            System.out.println("✓ Database initialization complete\n");

            System.out.println("Step 3: Setting up primary stage...");
            primaryStage = stage;
            SceneManager.getInstance().init(primaryStage);
            System.out.println("✓ Stage initialized\n");

            System.out.println("Step 4: Loading main scene...");
            SceneManager.getInstance().loadMain(); // load main menu
            System.out.println("✓ Main scene loaded\n");

            System.out.println("Step 5: Showing window...");
            stage.setTitle("JetStream Airline");
            stage.setWidth(800);
            stage.setHeight(600);
            stage.show();
            System.out.println("✓ Application window displayed\n");

            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║          Application Started Successfully!                 ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println("\n✗✗✗ CRITICAL ERROR DURING STARTUP ✗✗✗");
            System.err.println("Error: " + e.getMessage());
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
        // Force all silent FXML exceptions to print in the console
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("✗✗✗ UNCAUGHT EXCEPTION IN THREAD: " + t.getName());
            e.printStackTrace();
        });
        launch();
    }
}
