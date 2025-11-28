package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.services.BookingService;
import com.jetstream.utils.SceneManager;
import com.jetstream.models.Booking;
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
 * Controller for Booking Management screen
 */
public class BookingManagementController {

    private static final Logger logger = Logger.getLogger(BookingManagementController.class.getName());

    @FXML private TextField txtSearchPNR, txtSearchCustomer;
    @FXML private ComboBox<String> cmbSearchStatus;
    @FXML private Button btnSearch, btnClearSearch, btnViewDetails, btnCancelBooking, btnConfirmBooking;
    @FXML private Label totalBookingsLabel, confirmedBookingsLabel, cancelledBookingsLabel, totalRevenueLabel;
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> colPNR, colCustomerName, colFlightNumber, colSeatNumber, colClass, colStatus, colBookingDate, colAmount;

    private final BookingService bookingService = new BookingService();
    private final ObservableList<Booking> allBookingsList = FXCollections.observableArrayList();
    private FilteredList<Booking> filteredBookingsList;
    private Booking selectedBooking;

    @FXML
    public void initialize() {
        logger.info("BookingManagementController initialized");

        // Setup combo box
        cmbSearchStatus.setItems(FXCollections.observableArrayList("All", "confirmed", "cancelled", "pending"));
        cmbSearchStatus.setValue("All");

        // Setup table columns
        colPNR.setCellValueFactory(new PropertyValueFactory<>("pnrNumber"));
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("passengerName"));
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
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Setup table selection listener
        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedBooking = newSelection;
        });

        // Setup filtered list
        filteredBookingsList = new FilteredList<>(allBookingsList, p -> true);
        bookingsTable.setItems(filteredBookingsList);

        // Load data
        loadBookings();
        loadStatistics();

        // If the logged-in user is staff, disable admin-only actions
        try {
            com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
            if (session.isStaff()) {
                btnCancelBooking.setDisable(true);
                btnConfirmBooking.setDisable(true);
            }
        } catch (Exception ex) {
            // ignore - session not initialized
        }
    }

    /**
     * Load bookings from database
     */
    private void loadBookings() {
        try {
            allBookingsList.clear();
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "SELECT b.id, b.pnr_number, b.customer_id, b.flight_id, b.seat_id, " +
                           "b.status, b.booking_date, b.final_amount, " +
                           "c.cust_name, f.flight_name as flight_number, " +
                           "s.seat_number " +
                           "FROM bookings b " +
                           "LEFT JOIN customer_details c ON b.customer_id = c.id " +
                           "LEFT JOIN flights f ON b.flight_id = f.id " +
                           "LEFT JOIN seats s ON b.seat_id = s.id " +
                           "ORDER BY b.booking_date DESC";

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setId(rs.getInt("id"));
                    booking.setPnrNumber(rs.getString("pnr_number"));
                    booking.setCustomerId(rs.getInt("customer_id"));
                    booking.setFlightId(rs.getInt("flight_id"));
                    booking.setPassengerName(rs.getString("cust_name"));
                    booking.setFlightNumber(rs.getString("flight_number"));
                    booking.setSeatNumber(rs.getString("seat_number"));
                    booking.setStatus(rs.getString("status"));
                    booking.setBookingDate(rs.getTimestamp("booking_date") != null ?
                        rs.getTimestamp("booking_date").toString() : "N/A");
                    booking.setAmount(rs.getDouble("final_amount"));

                    allBookingsList.add(booking);
                }

                rs.close();
                stmt.close();
            }

            logger.info("Loaded " + allBookingsList.size() + " bookings");

        } catch (Exception e) {
            logger.severe("Error loading bookings: " + e.getMessage());
            showAlert("Error", "Failed to load bookings: " + e.getMessage());
        }
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        try {
            int totalBookings = allBookingsList.size();
            int confirmedBookings = (int) allBookingsList.stream()
                .filter(b -> "confirmed".equals(b.getStatus()))
                .count();
            int cancelledBookings = (int) allBookingsList.stream()
                .filter(b -> "cancelled".equals(b.getStatus()))
                .count();

            double totalRevenue = allBookingsList.stream()
                .filter(b -> "confirmed".equals(b.getStatus()))
                .mapToDouble(Booking::getAmount)
                .sum();

            totalBookingsLabel.setText(String.valueOf(totalBookings));
            confirmedBookingsLabel.setText(String.valueOf(confirmedBookings));
            cancelledBookingsLabel.setText(String.valueOf(cancelledBookings));
            totalRevenueLabel.setText(String.format("R %.2f", totalRevenue));

        } catch (Exception e) {
            logger.warning("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Handle search
     */
    @FXML
    private void handleSearch() {
        String pnrFilter = txtSearchPNR.getText().trim().toLowerCase();
        String customerFilter = txtSearchCustomer.getText().trim().toLowerCase();
        String statusFilter = cmbSearchStatus.getValue();

        filteredBookingsList.setPredicate(booking -> {
            boolean matchesPNR = pnrFilter.isEmpty() || booking.getPnrNumber().toLowerCase().contains(pnrFilter);
            boolean matchesCustomer = customerFilter.isEmpty() ||
                (booking.getPassengerName() != null && booking.getPassengerName().toLowerCase().contains(customerFilter));
            boolean matchesStatus = "All".equals(statusFilter) || statusFilter.equals(booking.getStatus());

            return matchesPNR && matchesCustomer && matchesStatus;
        });
    }

    /**
     * Handle clear search
     */
    @FXML
    private void handleClearSearch() {
        txtSearchPNR.clear();
        txtSearchCustomer.clear();
        cmbSearchStatus.setValue("All");
        filteredBookingsList.setPredicate(p -> true);
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

        String details = String.format(
            "Booking Details:\n\n" +
            "PNR: %s\n" +
            "Customer: %s\n" +
            "Flight: %s\n" +
            "Seat: %s\n" +
            "Status: %s\n" +
            "Booking Date: %s\n" +
            "Amount: R %.2f",
            selectedBooking.getPnrNumber(),
            selectedBooking.getPassengerName(),
            selectedBooking.getFlightNumber(),
            selectedBooking.getSeatNumber(),
            selectedBooking.getStatus(),
            selectedBooking.getBookingDate(),
            selectedBooking.getAmount()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Details");
        alert.setHeaderText("Detailed Information");
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * Handle cancel booking
     */
    @FXML
    private void handleCancelBooking() {
        if (selectedBooking == null) {
            showAlert("Error", "Please select a booking to cancel");
            return;
        }

        if ("cancelled".equals(selectedBooking.getStatus())) {
            showAlert("Error", "Booking is already cancelled");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cancellation");
        confirmation.setHeaderText("Cancel Booking");
        confirmation.setContentText("Are you sure you want to cancel booking " + selectedBooking.getPnrNumber() + "?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Connection conn = DatabaseConnection.getConnection();
                    if (conn != null) {
                        String sql = "UPDATE bookings SET status = 'cancelled' WHERE id = ?";

                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, selectedBooking.getId());

                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            showAlert("Success", "Booking cancelled successfully!");
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
     * Handle confirm booking
     */
    @FXML
    private void handleConfirmBooking() {
        if (selectedBooking == null) {
            showAlert("Error", "Please select a booking to confirm");
            return;
        }

        if ("confirmed".equals(selectedBooking.getStatus())) {
            showAlert("Error", "Booking is already confirmed");
            return;
        }

        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "UPDATE bookings SET status = 'confirmed' WHERE id = ?";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedBooking.getId());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Booking confirmed successfully!");
                    loadBookings();
                    loadStatistics();
                } else {
                    showAlert("Error", "Failed to confirm booking");
                }

                stmt.close();
            }

        } catch (Exception e) {
            logger.severe("Error confirming booking: " + e.getMessage());
            showAlert("Error", "Failed to confirm booking: " + e.getMessage());
        }
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBackToDashboard() {
        // Route back to staff dashboard if a staff user is logged in
        com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
        if (session.isStaff()) {
            SceneManager.getInstance().loadStaffDashboard();
        } else {
            SceneManager.getInstance().loadAdminDashboard();
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
