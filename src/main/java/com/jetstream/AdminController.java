package com.jetstream.controllers;

import com.jetstream.services.AdminService;
import com.jetstream.services.BookingService;
import com.jetstream.services.CancellationService;
import com.jetstream.services.FlightService;
import com.jetstream.models.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class AdminController extends BaseController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Button btnBack;

    @FXML private VBox loginForm;
    @FXML private HBox cardContainer;
    @FXML private Text txtTotalFlights;
    @FXML private Text txtTotalBookings;
    @FXML private Text txtTotalCancellations;

    private final AdminService adminService = new AdminService();
    private final FlightService flightService = new FlightService();
    private final BookingService bookingService = new BookingService();
    private final CancellationService cancellationService = new CancellationService();

    @FXML
    private void onLogin() {
        String u = txtUsername.getText();
        String p = txtPassword.getText();

        if (u == null || u.isEmpty() || p == null || p.isEmpty()) {
            showAlert("Validation", "Enter username and password.");
            return;
        }

        User authenticatedUser = adminService.authenticate(u, p);
        if (authenticatedUser != null) {
            showAlert("Success", "Authenticated as " + authenticatedUser.getRole() + ".");
            showDashboardCards();
        } else {
            showAlert("Failure", "Invalid credentials.");
        }
    }

    @FXML
    private void onBack() {
        // Route to the new Admin Dashboard instead of legacy main.fxml
        goTo("admin_dashboard.fxml");
    }

    private void showDashboardCards() {
        // Hide login form and show cards
        loginForm.setVisible(false);
        cardContainer.setVisible(true);

        try {
            txtTotalFlights.setText(String.valueOf(flightService.getTotalFlights()));
            txtTotalBookings.setText(String.valueOf(bookingService.getTotalBookings()));
            txtTotalCancellations.setText(String.valueOf(cancellationService.getTotalCancellations()));
        } catch (Exception e) {
            showAlert("Error", "Failed to load metrics.");
        }

        // Fade-in animation
        for (Text cardValue : new Text[]{txtTotalFlights, txtTotalBookings, txtTotalCancellations}) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1), cardValue);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }
}
