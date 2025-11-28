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

    public void loadLogin() {
        loadScene("/fxml/login.fxml");
    }

    public void loadRegister() {
        loadScene("/fxml/register.fxml");
    }

    public void loadAdminDashboard() {
        loadScene("/fxml/admin_dashboard.fxml");
    }

    public void loadStaffDashboard() {
        loadScene("/fxml/staff_dashboard.fxml");
    }

    public void loadCustomerDashboard() {
        loadScene("/fxml/customer_dashboard.fxml");
    }

    public void loadReservation() {
        loadScene("/fxml/reservation.fxml");
    }

    public void loadMyBookings() {
        loadScene("/fxml/my_bookings.fxml");
    }

    public void loadMyCancellations() {
        loadScene("/fxml/my_cancellations.fxml");
    }

    public void loadFlightManagement() {
        loadScene("/fxml/flight_management.fxml");
    }

    public void loadUserManagement() {
        loadScene("/fxml/user_management.fxml");
    }

    public void loadBookingManagement() {
        loadScene("/fxml/booking_management.fxml");
    }

    public void loadCancellationManagement() {
        loadScene("/fxml/cancellation_management.fxml");
    }

    public void loadReports() {
        loadScene("/fxml/reports.fxml");
    }

    public void loadSettings() {
        loadScene("/fxml/settings.fxml");
    }

    public void loadScene(String fxmlFile) {
        try {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("Loading FXML: " + fxmlFile);

            // Check if resource exists
            if (getClass().getResource(fxmlFile) == null) {
                System.err.println("✗ FXML file not found: " + fxmlFile);
                System.err.println("  Make sure the file exists in src/main/resources" + fxmlFile);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            if (root == null) {
                System.err.println("✗ FXML root is NULL!");
                return;
            } else {
                System.out.println("✓ FXML root loaded successfully!");
                System.out.println("  Root type: " + root.getClass().getSimpleName());
            }

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 800, 600);
                stage.setScene(scene);
                System.out.println("✓ New scene created (800x600)");
            } else {
                applyFade(root);
                stage.getScene().setRoot(root);
                System.out.println("✓ Scene root updated");
            }

            // attach stylesheet - try-catch to handle if CSS is missing
            try {
                stage.getScene().getStylesheets().clear();
                if (getClass().getResource("/css/style.css") != null) {
                    String cssPath = getClass().getResource("/css/style.css").toExternalForm();
                    stage.getScene().getStylesheets().add(cssPath);
                    System.out.println("✓ CSS loaded: " + cssPath);
                } else {
                    System.err.println("⚠ Warning: CSS file not found at /css/style.css");
                }
            } catch (Exception cssEx) {
                System.err.println("⚠ Warning: Could not load CSS file: " + cssEx.getMessage());
                // Continue without CSS - app will still work, just without styling
            }

            System.out.println("✓ Scene loaded successfully!");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        } catch (IOException e) {
            System.err.println("✗✗✗ ERROR LOADING FXML: " + fxmlFile);
            System.err.println("Exception: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            // Use HelloApplication.showAlert if needed, but avoid cyclic dependency here
        } catch (Exception e) {
            System.err.println("✗✗✗ UNEXPECTED ERROR LOADING FXML: " + fxmlFile);
            e.printStackTrace();
        }
    }

    private void applyFade(Parent root) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}
