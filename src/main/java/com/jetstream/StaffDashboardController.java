
package com.jetstream.controllers;

import com.jetstream.utils.SceneManager;
import com.jetstream.manager.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

/**
 * Controller for the staff dashboard. Provides limited navigation for staff users.
 */
public class StaffDashboardController {

    @FXML
    private Label lblUser;

    @FXML
    private Button btnLogout;

    @FXML
    public void initialize() {
        try {
            SessionManager session = SessionManager.getInstance();
            if (session != null && session.isLoggedIn()) {
                lblUser.setText(session.getDisplayName() + " (" + session.getRoleDisplay() + ")");
            }

            btnLogout.setOnAction(e -> handleLogout());
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void handleViewBookings() {
        SceneManager.getInstance().loadBookingManagement();
    }

    @FXML
    private void handleViewCancellations() {
        SceneManager.getInstance().loadCancellationManagement();
    }

    @FXML
    private void handleViewFlights() {
        SceneManager.getInstance().loadFlightManagement();
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().logout();
        } catch (Exception ignored) {
        }
        SceneManager.getInstance().loadLogin();
    }

    @FXML
    private void handleBack() {
        // If there's a previous page or landing page, navigate appropriately
        // Here, we route back to staff dashboard itself for now
        SceneManager.getInstance().loadStaffDashboard();
    }
}
