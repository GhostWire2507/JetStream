package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.manager.SessionManager;
import com.jetstream.models.Booking;

import com.jetstream.services.BookingService;
import com.jetstream.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Controller for Customer My Bookings screen
 */
public class MyBookingsController {

    private static final Logger logger = Logger.getLogger(MyBookingsController.class.getName());

    @FXML private ComboBox<String> statusFilter;
    @FXML private Button btnFilter, btnClearFilter, btnNewBooking, btnCancelBooking;
    @FXML private Label totalBookingsLabel, activeBookingsLabel, cancelledBookingsLabel, totalSpentLabel;
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> colPNR, colFlightNumber, colSeatNumber, colClass, colStatus, colBookingDate, colAmount, colActions;

    private final BookingService bookingService = new BookingService();
    private final ObservableList<Booking> allBookingsList = FXCollections.observableArrayList();
    private FilteredList<Booking> filteredBookingsList;
    private Booking selectedBooking;

    @FXML
    public void initialize() {
        logger.info("MyBookingsController initialized");

        // Setup filter combo box
        statusFilter.setItems(FXCollections.observableArrayList("All", "confirmed", "cancelled", "pending"));
        statusFilter.setValue("All");

        // Setup table columns
        colPNR.setCellValueFactory(new PropertyValueFactory<>("pnrNumber"));
        colFlightNumber.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        colSeatNumber.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colClass.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            // Determine class based on seat number (assuming E for economy, others executive)
            String seatNumber = booking.getSeatNumber();
            String seatClass = (seatNumber != null && seatNumber.startsWith("E")) ? "Economy" : "Executive";
            return new javafx.beans.property.SimpleStringProperty(seatClass);
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colBookingDate.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        colAmount.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty("R " + String.format("%.2f", booking.getAmount()));
        });

        // Setup actions column
        colActions.setCellFactory(column -> new TableCell<Booking, String>() {
            private final Button viewBtn = new Button("View");
            private final Button cancelBtn = new Button("Cancel");

            {
                viewBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-size: 10;");
                cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 10;");

                viewBtn.setOnAction(event -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    handleViewBooking(booking);
                });

                cancelBtn.setOnAction(event -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    handleCancelBooking(booking);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Booking booking = getTableView().getItems().get(getIndex());
                    HBox hbox = new HBox(5, viewBtn, cancelBtn);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });

        // Setup table selection
        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedBooking = newSelection;
        });

        // Load initial data
        loadBookings();
        loadStatistics();
    }

    /**
     * Load customer's bookings
     */
    private void loadBookings() {
        try {
            // Get current user email from session
            String userEmail = SessionManager.getInstance().getEmail();
            if (userEmail == null || userEmail.isEmpty()) {
                return;
            }
            allBookingsList.clear();

            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "SELECT b.id, b.pnr_number, b.class_code, b.seat_id, b.flight_id, b.status, b.booking_date, b.final_amount, f.flight_number " +
                           "FROM bookings b " +
                           "LEFT JOIN flights f ON b.flight_id = f.id " +
                           "WHERE b.customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?)) " +
                           "ORDER BY b.booking_date DESC";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, userEmail);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setId(rs.getInt("id"));
                    booking.setPnrNumber(rs.getString("pnr_number"));
                    booking.setPassengerName("");
                    booking.setPassengerEmail("");
                    booking.setFlightNumber(rs.getString("flight_number"));
                    booking.setSeatNumber(rs.getString("seat_id"));
                    booking.setStatus(rs.getString("status"));
                    booking.setBookingDate(rs.getTimestamp("booking_date") != null ? rs.getTimestamp("booking_date").toString() : "");
                    booking.setAmount(rs.getDouble("final_amount"));
                    booking.setFlightId(rs.getInt("flight_id"));

                    allBookingsList.add(booking);
                }

                rs.close();
                stmt.close();
            }

            filteredBookingsList = new FilteredList<>(allBookingsList);
            bookingsTable.setItems(filteredBookingsList);

        } catch (Exception e) {
            logger.severe("Error loading bookings: " + e.getMessage());
            showAlert("Error", "Failed to load bookings: " + e.getMessage());
        }
    }

    /**
     * Load booking statistics
     */
    private void loadStatistics() {
        try {
            String userEmail = SessionManager.getInstance().getEmail();
            if (userEmail == null || userEmail.isEmpty()) {
                return;
            }

            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                // Total bookings
                String totalSql = "SELECT COUNT(*) FROM bookings WHERE customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?))";
                PreparedStatement totalStmt = conn.prepareStatement(totalSql);
                totalStmt.setString(1, userEmail);
                ResultSet totalRs = totalStmt.executeQuery();
                int totalBookings = totalRs.next() ? totalRs.getInt(1) : 0;
                totalBookingsLabel.setText(String.valueOf(totalBookings));
                totalRs.close();
                totalStmt.close();

                // Active bookings
                String activeSql = "SELECT COUNT(*) FROM bookings WHERE customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?)) AND status = 'confirmed'";
                PreparedStatement activeStmt = conn.prepareStatement(activeSql);
                activeStmt.setString(1, userEmail);
                ResultSet activeRs = activeStmt.executeQuery();
                int activeBookings = activeRs.next() ? activeRs.getInt(1) : 0;
                activeBookingsLabel.setText(String.valueOf(activeBookings));
                activeRs.close();
                activeStmt.close();

                // Cancelled bookings
                String cancelledSql = "SELECT COUNT(*) FROM bookings WHERE customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?)) AND status = 'cancelled'";
                PreparedStatement cancelledStmt = conn.prepareStatement(cancelledSql);
                cancelledStmt.setString(1, userEmail);
                ResultSet cancelledRs = cancelledStmt.executeQuery();
                int cancelledBookings = cancelledRs.next() ? cancelledRs.getInt(1) : 0;
                cancelledBookingsLabel.setText(String.valueOf(cancelledBookings));
                cancelledRs.close();
                cancelledStmt.close();

                // Total spent
                String spentSql = "SELECT SUM(final_amount) FROM bookings WHERE customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?)) AND status = 'confirmed'";
                PreparedStatement spentStmt = conn.prepareStatement(spentSql);
                spentStmt.setString(1, userEmail);
                ResultSet spentRs = spentStmt.executeQuery();
                double totalSpent = spentRs.next() ? spentRs.getDouble(1) : 0.0;
                totalSpentLabel.setText("R " + String.format("%.2f", totalSpent));
                spentRs.close();
                spentStmt.close();
            }

        } catch (Exception e) {
            logger.severe("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Handle filter button
     */
    @FXML
    private void handleFilter() {
        String selectedStatus = statusFilter.getValue();
        if ("All".equals(selectedStatus)) {
            filteredBookingsList.setPredicate(null);
        } else {
            filteredBookingsList.setPredicate(booking -> selectedStatus.equals(booking.getStatus()));
        }
    }

    /**
     * Handle clear filter button
     */
    @FXML
    private void handleClearFilter() {
        statusFilter.setValue("All");
        filteredBookingsList.setPredicate(null);
    }

    /**
     * Handle new booking button
     */
    @FXML
    private void handleNewBooking() {
        SceneManager.getInstance().loadReservation();
    }

    /**
     * Handle cancel booking button (redirects to cancellation view)
     */
    @FXML
    private void handleCancelBooking() {
        // Navigate to customer-specific cancellations view instead of admin management
        SceneManager.getInstance().loadMyCancellations();
    }

    /**
     * Handle view booking details
     */
    private void handleViewBooking(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Details");
        alert.setHeaderText("Booking Information");
        alert.setContentText(
            "PNR: " + booking.getPnrNumber() + "\n" +
            "Flight: " + booking.getFlightNumber() + "\n" +
            "Seat: " + booking.getSeatNumber() + "\n" +
            "Status: " + booking.getStatus() + "\n" +
            "Amount: R " + String.format("%.2f", booking.getAmount()) + "\n" +
            "Booking Date: " + booking.getBookingDate()
        );
        alert.showAndWait();
    }

    /**
     * Handle cancel booking (individual booking)
     */
    private void handleCancelBooking(Booking booking) {
        if (!"confirmed".equals(booking.getStatus())) {
            showAlert("Error", "Only confirmed bookings can be cancelled");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Booking");
        confirmation.setHeaderText("Cancel Booking Confirmation");
        confirmation.setContentText("Are you sure you want to cancel this booking?\n\n" +
                                  "PNR: " + booking.getPnrNumber() + "\n" +
                                  "Flight: " + booking.getFlightNumber() + "\n" +
                                  "Refund Amount: R " + String.format("%.2f", booking.getAmount() * 0.8));

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Connection conn = DatabaseConnection.getConnection();
                    if (conn != null) {
                        String sql = "UPDATE bookings SET status = 'cancelled' WHERE id = ?";

                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, booking.getId());

                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            showAlert("Success", "Booking cancelled successfully!\nRefund amount: R " +
                                     String.format("%.2f", booking.getAmount() * 0.8));
                            loadBookings();
                            loadStatistics();
                        } else {
                            showAlert("Error", "Failed to cancel booking");
                        }

                        stmt.close();
                    }

                } catch (Exception e) {
                    logger.severe("Error cancelling booking: " + e.getMessage());
                    showAlert("Error", "Failed to cancel booking: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBackToDashboard() {
        com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
        if (session.isStaff()) {
            SceneManager.getInstance().loadStaffDashboard();
        } else if (session.isAdmin()) {
            SceneManager.getInstance().loadAdminDashboard();
        } else {
            SceneManager.getInstance().loadCustomerDashboard();
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
