package com.jetstream.controller;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.manager.SessionManager;
import com.jetstream.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Controller for Admin Dashboard
 */
public class AdminDashboardController {

    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());

    @FXML private Label welcomeLabel;
    @FXML private Label totalFlightsLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private VBox activityContainer;
    @FXML private ProgressBar systemProgressBar;
    @FXML private Label systemStatusLabel;

    @FXML
    public void initialize() {
        logger.info("AdminDashboardController initialized");
        
        // Set welcome message
        SessionManager session = SessionManager.getInstance();
        welcomeLabel.setText("Welcome, " + session.getDisplayName());

        // Load dashboard data
        loadDashboardMetrics();
        loadRecentActivity();
    }

    /**
     * Load dashboard metrics from database
     */
    private void loadDashboardMetrics() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) {
                logger.warning("Database not available");
                return;
            }

            Statement stmt = conn.createStatement();

            // Total Flights
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM flights");
            if (rs.next()) {
                totalFlightsLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Total Bookings
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM bookings WHERE status = 'confirmed'");
            if (rs.next()) {
                totalBookingsLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Total Users
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM users WHERE role = 'customer'");
            if (rs.next()) {
                totalUsersLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Total Revenue
            rs = stmt.executeQuery("SELECT COALESCE(SUM(final_amount), 0) as total FROM bookings WHERE status = 'confirmed'");
            if (rs.next()) {
                double revenue = rs.getDouble("total");
                totalRevenueLabel.setText(String.format("R %.2f", revenue));
            }

            rs.close();
            stmt.close();

            logger.info("Dashboard metrics loaded successfully");

        } catch (Exception e) {
            logger.severe("Error loading dashboard metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load recent activity
     */
    private void loadRecentActivity() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT b.pnr_number, c.cust_name, f.flight_number, b.booking_date " +
                "FROM bookings b " +
                "JOIN customer_details c ON b.customer_id = c.id " +
                "JOIN flights f ON b.flight_id = f.id " +
                "ORDER BY b.booking_date DESC LIMIT 5"
            );

            activityContainer.getChildren().clear();

            while (rs.next()) {
                String pnr = rs.getString("pnr_number");
                String customer = rs.getString("cust_name");
                String flight = rs.getString("flight_number");
                String date = rs.getTimestamp("booking_date").toString();

                Label activityLabel = new Label(
                    String.format("🎫 %s - %s booked flight %s", pnr, customer, flight)
                );
                activityLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
                activityContainer.getChildren().add(activityLabel);
            }

            rs.close();
            stmt.close();

        } catch (Exception e) {
            logger.warning("Error loading recent activity: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.getInstance().loadLogin();
    }

    @FXML
    private void showDashboard() {
        // Already on dashboard
    }

    @FXML
    private void showFlights() {
        try {
            SceneManager.getInstance().loadFlightManagement();
        } catch (Exception e) {
            logger.severe("Failed to load flight management: " + e.getMessage());
            showAlert("Error", "Failed to load flight management screen");
        }
    }

    @FXML
    private void showUsers() {
        try {
            SceneManager.getInstance().loadUserManagement();
        } catch (Exception e) {
            logger.severe("Failed to load user management: " + e.getMessage());
            showAlert("Error", "Failed to load user management screen");
        }
    }

    @FXML
    private void showBookings() {
        try {
            SceneManager.getInstance().loadBookingManagement();
        } catch (Exception e) {
            logger.severe("Failed to load booking management: " + e.getMessage());
            showAlert("Error", "Failed to load booking management screen");
        }
    }

    @FXML
    private void showCancellations() {
        try {
            SceneManager.getInstance().loadCancellationManagement();
        } catch (Exception e) {
            logger.severe("Failed to load cancellation management: " + e.getMessage());
            showAlert("Error", "Failed to load cancellation management screen");
        }
    }

    @FXML
    private void showReports() {
        try {
            SceneManager.getInstance().loadReports();
        } catch (Exception e) {
            logger.severe("Failed to load reports: " + e.getMessage());
            showAlert("Error", "Failed to load reports screen");
        }
    }

    @FXML
    private void showSettings() {
        try {
            SceneManager.getInstance().loadSettings();
        } catch (Exception e) {
            logger.severe("Failed to load settings: " + e.getMessage());
            showAlert("Error", "Failed to load settings screen");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

