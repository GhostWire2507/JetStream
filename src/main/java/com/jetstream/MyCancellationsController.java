package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.manager.SessionManager;
import com.jetstream.models.Booking;
import com.jetstream.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Controller for Customer My Cancellations screen
 */
public class MyCancellationsController {

    private static final Logger logger = Logger.getLogger(MyCancellationsController.class.getName());

    @FXML private Label welcomeLabel;
    @FXML private Button btnCancelSelected, btnViewDetails, btnBack;
    @FXML private Label totalCancellableLabel, refundableAmountLabel, cancellationFeeLabel;
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> colPNR, colFlightNumber, colSeatNumber, colBookingDate, colDepartureDate, colAmount, colRefundAmount;

    private final ObservableList<Booking> cancellableBookingsList = FXCollections.observableArrayList();
    private FilteredList<Booking> filteredBookingsList;
    private Booking selectedBooking;

    @FXML
    public void initialize() {
        logger.info("MyCancellationsController initialized");

        // Set welcome message
        String fullName = SessionManager.getInstance().getFullName();
        welcomeLabel.setText("Welcome, " + fullName);

        // Setup table columns
        colPNR.setCellValueFactory(new PropertyValueFactory<>("pnrNumber"));
        colFlightNumber.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        colSeatNumber.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colBookingDate.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        colDepartureDate.setCellValueFactory(cellData -> {
            // For now, return booking date as placeholder
            return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getBookingDate());
        });
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colRefundAmount.setCellValueFactory(cellData -> {
            // Calculate refund amount (assume 80% refund for simplicity)
            double refund = cellData.getValue().getAmount() * 0.8;
            return new javafx.beans.property.SimpleStringProperty(String.format("R %.2f", refund));
        });

        // Setup table selection listener
        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedBooking = newSelection;
        });

        // Setup filtered list (only show confirmed bookings that can be cancelled)
        filteredBookingsList = new FilteredList<>(cancellableBookingsList, p -> true);
        bookingsTable.setItems(filteredBookingsList);

        // Load data
        loadCancellableBookings();
        loadStatistics();
    }

    /**
     * Load customer's cancellable bookings from database
     */
    private void loadCancellableBookings() {
        try {
            cancellableBookingsList.clear();
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                int customerId = SessionManager.getInstance().getUserId();
                String sql = "SELECT b.id, b.pnr_number, b.flight_id, b.seat_id, b.travel_date, " +
                           "b.status, b.booking_date, b.final_amount, b.class_code, " +
                           "f.flight_name as flight_number, f.flight_number as flight_code, " +
                           "cd.cust_name as passenger_name, s.seat_number " +
                           "FROM bookings b " +
                           "LEFT JOIN flights f ON b.flight_id = f.id " +
                           "LEFT JOIN customer_details cd ON b.customer_id = cd.id " +
                           "LEFT JOIN seats s ON b.seat_id = s.id " +
                           "WHERE b.customer_id = ? AND b.status = 'confirmed' " +
                           "ORDER BY b.booking_date DESC";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, customerId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setId(rs.getInt("id"));
                    booking.setPnrNumber(rs.getString("pnr_number"));
                    booking.setFlightId(rs.getInt("flight_id"));
                    booking.setSeatNumber(rs.getString("seat_number"));
                    booking.setStatus(rs.getString("status"));
                    booking.setBookingDate(rs.getTimestamp("booking_date") != null ?
                        rs.getTimestamp("booking_date").toString() : "N/A");
                    booking.setAmount(rs.getDouble("final_amount"));
                    booking.setFlightNumber(rs.getString("flight_code"));
                    booking.setPassengerName(rs.getString("passenger_name"));
                    // Store additional data needed for cancellation
                    booking.setTravelDate(rs.getString("travel_date"));
                    booking.setClassCode(rs.getString("class_code"));

                    cancellableBookingsList.add(booking);
                }

                rs.close();
                stmt.close();
            }

            logger.info("Loaded " + cancellableBookingsList.size() + " cancellable bookings for customer");

        } catch (Exception e) {
            logger.severe("Error loading cancellable bookings: " + e.getMessage());
            showAlert("Error", "Failed to load bookings: " + e.getMessage());
        }
    }

    /**
     * Load statistics for cancellation
     */
    private void loadStatistics() {
        try {
            int totalCancellable = cancellableBookingsList.size();
            double totalRefundable = cancellableBookingsList.stream()
                .mapToDouble(booking -> booking.getAmount() * 0.8) // 80% refund
                .sum();
            double totalFees = cancellableBookingsList.stream()
                .mapToDouble(booking -> booking.getAmount() * 0.2) // 20% fee
                .sum();

            totalCancellableLabel.setText(String.valueOf(totalCancellable));
            refundableAmountLabel.setText(String.format("R %.2f", totalRefundable));
            cancellationFeeLabel.setText(String.format("R %.2f", totalFees));

        } catch (Exception e) {
            logger.warning("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Handle cancel selected booking
     */
    @FXML
    private void handleCancelSelected() {
        if (selectedBooking == null) {
            showAlert("Error", "Please select a booking to cancel");
            return;
        }

        double refundAmount = selectedBooking.getAmount() * 0.8;
        double feeAmount = selectedBooking.getAmount() * 0.2;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cancellation");
        confirmation.setHeaderText("Cancel Booking " + selectedBooking.getPnrNumber());
        confirmation.setContentText(String.format(
            "Are you sure you want to cancel this booking?\n\n" +
            "Original Amount: R %.2f\n" +
            "Refund Amount: R %.2f\n" +
            "Cancellation Fee: R %.2f\n\n" +
            "This action cannot be undone.",
            selectedBooking.getAmount(), refundAmount, feeAmount
        ));

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cancelBooking(selectedBooking);
            }
        });
    }

    /**
     * Cancel the selected booking
     */
    private void cancelBooking(Booking booking) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "UPDATE bookings SET status = 'cancelled' WHERE id = ?";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, booking.getId());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Booking cancelled successfully!\n\n" +
                        "Refund of R " + String.format("%.2f", booking.getAmount() * 0.8) +
                        " will be processed within 5-7 business days.");
                    loadCancellableBookings();
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

    /**
     * Handle view details
     */
    @FXML
    private void handleViewDetails() {
        if (selectedBooking == null) {
            showAlert("Error", "Please select a booking to view details");
            return;
        }

        double refundAmount = selectedBooking.getAmount() * 0.8;
        double feeAmount = selectedBooking.getAmount() * 0.2;

        String details = String.format(
            "Booking Details:\n\n" +
            "PNR: %s\n" +
            "Flight: %s\n" +
            "Seat: %s\n" +
            "Booking Date: %s\n" +
            "Original Amount: R %.2f\n" +
            "Refund Amount: R %.2f\n" +
            "Cancellation Fee: R %.2f",
            selectedBooking.getPnrNumber(),
            selectedBooking.getFlightNumber(),
            selectedBooking.getSeatNumber(),
            selectedBooking.getBookingDate(),
            selectedBooking.getAmount(),
            refundAmount,
            feeAmount
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Details");
        alert.setHeaderText("Cancellation Information");
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBack() {
        SceneManager.getInstance().loadCustomerDashboard();
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
