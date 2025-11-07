package com.jetstream.utils;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Singleton SceneManager responsible for loading scenes and applying transitions.
 */
public class SceneManager {

    private static SceneManager instance;
    private Stage stage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void init(Stage primaryStage) {
        this.stage = primaryStage;
    }

    public void loadMain() {
        loadScene("/fxml/main.fxml");
    }

    public void loadScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                applyFade(root);
                stage.getScene().setRoot(root);
            }
            // attach stylesheet
            stage.getScene().getStylesheets().clear();
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            // Use HelloApplication.showAlert if needed, but avoid cyclic dependency here
        }
    }

    private void applyFade(Parent root) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}
