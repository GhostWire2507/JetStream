package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.manager.SessionManager;
import com.jetstream.services.BookingService;
import com.jetstream.services.CancellationService;
import com.jetstream.services.FlightService;
import com.jetstream.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Controller for Customer Dashboard screen
 */
public class CustomerDashboardController {

    private static final Logger logger = Logger.getLogger(CustomerDashboardController.class.getName());

    @FXML private Label welcomeLabel;
    @FXML private Label myBookingsLabel;
    @FXML private Label upcomingFlightsLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label myCancellationsLabel;
    @FXML private VBox activityContainer;

    private final BookingService bookingService = new BookingService();
    private final CancellationService cancellationService = new CancellationService();
    private final FlightService flightService = new FlightService();

    @FXML
    public void initialize() {
        logger.info("CustomerDashboardController initialized");

        // Set welcome message
        String fullName = SessionManager.getInstance().getFullName();
        welcomeLabel.setText("Welcome, " + fullName);

        // Load dashboard data
        loadDashboardData();
    }

    /**
     * Load customer dashboard data
     */
    private void loadDashboardData() {
        try {
            int customerId = SessionManager.getInstance().getUserId();
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) {
                logger.warning("No database connection available");
                return;
            }

            // Count total bookings for this customer
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM bookings WHERE customer_id = ?")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    myBookingsLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // Count upcoming flights (confirmed bookings with future departure)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM bookings b " +
                    "JOIN flights f ON b.flight_id = f.id " +
                    "WHERE b.customer_id = ? AND b.status = 'confirmed' " +
                    "AND f.departure_time > datetime('now')")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    upcomingFlightsLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // Calculate total spent (sum of final_amount for confirmed bookings)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(final_amount), 0) FROM bookings WHERE customer_id = ? AND status = 'confirmed'")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double totalSpent = rs.getDouble(1);
                    totalSpentLabel.setText(String.format("R %.2f", totalSpent));
                }
            }

            // Count cancellations for this customer
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM cancellations c " +
                    "JOIN bookings b ON c.booking_id = b.id " +
                    "WHERE b.customer_id = ?")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    myCancellationsLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // Load recent activity
            loadRecentActivity();

        } catch (Exception e) {
            logger.severe("Error loading dashboard data: " + e.getMessage());
        }
    }

    /**
     * Public method to refresh dashboard data (called after CRUD operations)
     */
    public void refreshDashboardData() {
        loadDashboardData();
    }

    /**
     * Load recent activity for the customer
     */
    private void loadRecentActivity() {
        try {
            // Clear existing activity
            activityContainer.getChildren().clear();

            int customerId = SessionManager.getInstance().getUserId();
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) {
                logger.warning("No database connection available for recent activity");
                return;
            }

            // Get recent bookings (last 5)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT b.pnr_number, f.flight_number, b.booking_date, b.status " +
                    "FROM bookings b " +
                    "JOIN flights f ON b.flight_id = f.id " +
                    "WHERE b.customer_id = ? " +
                    "ORDER BY b.booking_date DESC LIMIT 5")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                boolean hasActivity = false;
                while (rs.next()) {
                    hasActivity = true;
                    String activity = String.format("Booked flight %s (PNR: %s) on %s - %s",
                            rs.getString("flight_number"),
                            rs.getString("pnr_number"),
                            rs.getString("booking_date"),
                            rs.getString("status"));
                    Label activityLabel = new Label(activity);
                    activityLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
                    activityContainer.getChildren().add(activityLabel);
                }

                // Get recent cancellations (last 3)
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT c.pnr_number, c.cancellation_date, c.refund_amount " +
                        "FROM cancellations c " +
                        "JOIN bookings b ON c.booking_id = b.id " +
                        "WHERE b.customer_id = ? " +
                        "ORDER BY c.cancellation_date DESC LIMIT 3")) {
                    ps2.setInt(1, customerId);
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next()) {
                        hasActivity = true;
                        String activity = String.format("Cancelled booking (PNR: %s) on %s - Refund: R %.2f",
                                rs2.getString("pnr_number"),
                                rs2.getString("cancellation_date"),
                                rs2.getDouble("refund_amount"));
                        Label activityLabel = new Label(activity);
                        activityLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #f44336;");
                        activityContainer.getChildren().add(activityLabel);
                    }
                }

                // If no activity, show welcome message
                if (!hasActivity) {
                    Label activityLabel1 = new Label("Welcome to JetStream Airlines!");
                    activityLabel1.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
                    activityContainer.getChildren().add(activityLabel1);

                    Label activityLabel2 = new Label("Book your first flight to get started.");
                    activityLabel2.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
                    activityContainer.getChildren().add(activityLabel2);
                }

            }

        } catch (Exception e) {
            logger.warning("Error loading recent activity: " + e.getMessage());
        }
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.getInstance().loadLogin();
    }

    /**
     * Show dashboard (current view)
     */
    @FXML
    private void showDashboard() {
        // Already on dashboard
    }

    /**
     * Show reservation/booking screen
     */
    @FXML
    private void showReservation() {
        SceneManager.getInstance().loadScene("/fxml/reservation.fxml");
    }

    /**
     * Show customer's bookings
     */
    @FXML
    private void showMyBookings() {
        SceneManager.getInstance().loadScene("/fxml/my_bookings.fxml");
    }

    /**
     * Show customer's cancellations
     */
    @FXML
    private void showCancellation() {
        SceneManager.getInstance().loadScene("/fxml/my_cancellations.fxml");
    }

    /**
     * Show customer profile
     */
    @FXML
    private void showProfile() {
     
    }
}
