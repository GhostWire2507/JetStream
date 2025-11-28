package com.jetstream.controllers;

import com.jetstream.application.HelloApplication;
import com.jetstream.utils.SceneManager;

/**
 * BaseController with common utilities for controllers.
 * Controllers should extend this class.
 */
public abstract class BaseController {

    protected void showAlert(String title, String message) {
        HelloApplication.showAlert(title, message);
    }

    protected void goTo(String fxmlPath) {
        SceneManager.getInstance().loadScene("/fxml/" + fxmlPath);
    }
}
