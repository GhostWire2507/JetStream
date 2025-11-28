package com.jetstream.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ReportsController {
    private static final Logger logger = Logger.getLogger(ReportsController.class.getName());
    @FXML private Label totalFlightsLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label totalCancellationsLabel;
    @FXML private Label totalRevenueLabel;

    @FXML private TableView<BookingReportRow> recentBookingsTable;
    @FXML private TableColumn<BookingReportRow, Integer> colBookingId;
    @FXML private TableColumn<BookingReportRow, String> colUser;
    @FXML private TableColumn<BookingReportRow, String> colFlight;
    @FXML private TableColumn<BookingReportRow, String> colDate;
    @FXML private TableColumn<BookingReportRow, Double> colAmount;
    @FXML private TableColumn<BookingReportRow, String> colStatus;

    @FXML
    public void initialize() {
        loadStats();
        setupTable();
        loadRecentBookings();
    }

    private void loadStats() {
        Connection conn = com.jetstream.database.DatabaseConnection.getConnection();
        if (conn == null) return;
        // Total Flights
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM flights");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) totalFlightsLabel.setText(String.valueOf(rs.getInt(1)));
        } catch (SQLException e) { logger.fine("Failed to read total flights: " + e.getMessage()); }

        // Total Bookings
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM bookings");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) totalBookingsLabel.setText(String.valueOf(rs.getInt(1)));
        } catch (SQLException e) { logger.fine("Failed to read total bookings: " + e.getMessage()); }

        // Total Cancellations
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM cancellations");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) totalCancellationsLabel.setText(String.valueOf(rs.getInt(1)));
        } catch (SQLException e) { logger.fine("Failed to read total cancellations: " + e.getMessage()); }

        // Total Revenue (use final_amount and lowercase 'confirmed')
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(final_amount), 0) FROM bookings WHERE status = 'confirmed'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double revenue = rs.getDouble(1);
                totalRevenueLabel.setText("R " + String.format("%.2f", revenue));
            }
        } catch (SQLException e) { logger.fine("Failed to read total revenue: " + e.getMessage()); }
    }

    private void setupTable() {
        colBookingId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colFlight.setCellValueFactory(new PropertyValueFactory<>("flight"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadRecentBookings() {
        ObservableList<BookingReportRow> data = FXCollections.observableArrayList();
        Connection conn = com.jetstream.database.DatabaseConnection.getConnection();
        if (conn == null) return;
        // Fixed: use customer_id instead of user_id, final_amount instead of amount
        // Join with customer_details to get customer name
        String sql = "SELECT b.id, c.cust_name as customer_name, f.flight_number, b.booking_date, b.final_amount, b.status " +
                     "FROM bookings b " +
                     "LEFT JOIN customer_details c ON b.customer_id = c.id " +
                     "LEFT JOIN flights f ON b.flight_id = f.id " +
                     "ORDER BY b.booking_date DESC LIMIT 20";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.add(new BookingReportRow(
                    rs.getInt("id"),
                    rs.getString("customer_name"),
                    rs.getString("flight_number"),
                    rs.getString("booking_date"),
                    rs.getDouble("final_amount"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) { logger.fine("Failed to load recent bookings: " + e.getMessage()); }
        recentBookingsTable.setItems(data);
    }

    // Data class for table rows
    public static class BookingReportRow {
        private final Integer bookingId;
        private final String user;
        private final String flight;
        private final String date;
        private final Double amount;
        private final String status;
        public BookingReportRow(Integer bookingId, String user, String flight, String date, Double amount, String status) {
            this.bookingId = bookingId;
            this.user = user;
            this.flight = flight;
            this.date = date;
            this.amount = amount;
            this.status = status;
        }
        public Integer getBookingId() { return bookingId; }
        public String getUser() { return user; }
        public String getFlight() { return flight; }
        public String getDate() { return date; }
        public Double getAmount() { return amount; }
        public String getStatus() { return status; }
    }

    @FXML
    private void handleBackToDashboard() {
        com.jetstream.utils.SceneManager.getInstance().loadAdminDashboard();
    }
}
