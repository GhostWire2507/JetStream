package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.services.CancellationService;
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
 * Controller for Cancellation Management screen
 */
public class CancellationManagementController {

    private static final Logger logger = Logger.getLogger(CancellationManagementController.class.getName());

    @FXML private TextField txtSearchCustomer;
    @FXML private ComboBox<String> cmbSearchClass;
    @FXML private Button btnSearch, btnClearSearch, btnViewDetails, btnProcessRefund;
    @FXML private Label totalCancellationsLabel, pendingRefundsLabel, totalRefundAmountLabel;
    @FXML private TableView<CancellationManagementController.Cancellation> cancellationsTable;
    @FXML private TableColumn<CancellationManagementController.Cancellation, String> colCustomerCode, colClass, colSeatNumber, colRefundStatus;
    @FXML private TableColumn<CancellationManagementController.Cancellation, Integer> colDaysLeft, colHoursLeft;
    @FXML private TableColumn<CancellationManagementController.Cancellation, Double> colBasicAmount, colCancelAmount;

    private final CancellationService cancellationService = new CancellationService();
    private final ObservableList<CancellationManagementController.Cancellation> allCancellationsList = FXCollections.observableArrayList();
    private FilteredList<CancellationManagementController.Cancellation> filteredCancellationsList;
    private CancellationManagementController.Cancellation selectedCancellation;

    @FXML
    public void initialize() {
        logger.info("CancellationManagementController initialized");

        // Setup combo box
        cmbSearchClass.setItems(FXCollections.observableArrayList("All", "Economy", "Executive"));
        cmbSearchClass.setValue("All");

        // Setup table columns
        colCustomerCode.setCellValueFactory(new PropertyValueFactory<>("customerCode"));
        colClass.setCellValueFactory(new PropertyValueFactory<>("classType"));
        colSeatNumber.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colDaysLeft.setCellValueFactory(new PropertyValueFactory<>("daysLeft"));
        colHoursLeft.setCellValueFactory(new PropertyValueFactory<>("hoursLeft"));
        colBasicAmount.setCellValueFactory(new PropertyValueFactory<>("basicAmount"));
        colCancelAmount.setCellValueFactory(new PropertyValueFactory<>("cancelAmount"));
        colRefundStatus.setCellValueFactory(new PropertyValueFactory<>("refundStatus"));

        // Setup table selection listener
        cancellationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedCancellation = newSelection;
        });

        // Setup filtered list
        filteredCancellationsList = new FilteredList<>(allCancellationsList, p -> true);
        cancellationsTable.setItems(filteredCancellationsList);

        // Load data
        loadCancellations();
        loadStatistics();

        // If the logged-in user is staff, disable admin-only actions
        try {
            com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
            if (session.isStaff()) {
                btnProcessRefund.setDisable(true);
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    /**
     * Load cancellations from database
     */
    private void loadCancellations() {
        try {
            allCancellationsList.clear();
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "SELECT id, cust_code, class, seat_no, days_left, hours_left, basic_amount, cancel_amount FROM cancellations ORDER BY id DESC";

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    CancellationManagementController.Cancellation cancellation = new CancellationManagementController.Cancellation();
                    cancellation.setId(rs.getInt("id"));
                    cancellation.setCustomerCode(rs.getString("cust_code"));
                    cancellation.setClassType(rs.getString("class"));
                    cancellation.setSeatNumber(rs.getString("seat_no"));
                    cancellation.setDaysLeft(rs.getInt("days_left"));
                    cancellation.setHoursLeft(rs.getInt("hours_left"));
                    cancellation.setBasicAmount(rs.getDouble("basic_amount"));
                    cancellation.setCancelAmount(rs.getDouble("cancel_amount"));
                    cancellation.setRefundStatus("Pending"); // Default status

                    allCancellationsList.add(cancellation);
                }

                rs.close();
                stmt.close();
            }

            logger.info("Loaded " + allCancellationsList.size() + " cancellations");

        } catch (Exception e) {
            logger.severe("Error loading cancellations: " + e.getMessage());
            showAlert("Error", "Failed to load cancellations: " + e.getMessage());
        }
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        try {
            int totalCancellations = allCancellationsList.size();
            int pendingRefunds = (int) allCancellationsList.stream()
                .filter(c -> "Pending".equals(c.getRefundStatus()))
                .count();

            double totalRefundAmount = allCancellationsList.stream()
                .filter(c -> !"Pending".equals(c.getRefundStatus()))
                .mapToDouble(CancellationManagementController.Cancellation::getCancelAmount)
                .sum();

            totalCancellationsLabel.setText(String.valueOf(totalCancellations));
            pendingRefundsLabel.setText(String.valueOf(pendingRefunds));
            totalRefundAmountLabel.setText(String.format("R %.2f", totalRefundAmount));

        } catch (Exception e) {
            logger.warning("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Handle search
     */
    @FXML
    private void handleSearch() {
        String customerFilter = txtSearchCustomer.getText().trim().toLowerCase();
        String classFilter = cmbSearchClass.getValue();

        filteredCancellationsList.setPredicate(cancellation -> {
            boolean matchesCustomer = customerFilter.isEmpty() || cancellation.getCustomerCode().toLowerCase().contains(customerFilter);
            boolean matchesClass = "All".equals(classFilter) || classFilter.equals(cancellation.getClassType());

            return matchesCustomer && matchesClass;
        });
    }

    /**
     * Handle clear search
     */
    @FXML
    private void handleClearSearch() {
        txtSearchCustomer.clear();
        cmbSearchClass.setValue("All");
        filteredCancellationsList.setPredicate(p -> true);
    }

    /**
     * Handle view details
     */
    @FXML
    private void handleViewDetails() {
        if (selectedCancellation == null) {
            showAlert("Error", "Please select a cancellation to view details");
            return;
        }

        String details = String.format(
            "Cancellation Details:\n\n" +
            "Customer Code: %s\n" +
            "Class: %s\n" +
            "Seat Number: %s\n" +
            "Days Left: %d\n" +
            "Hours Left: %d\n" +
            "Basic Amount: R %.2f\n" +
            "Cancel Amount: R %.2f\n" +
            "Refund Status: %s",
            selectedCancellation.getCustomerCode(),
            selectedCancellation.getClassType(),
            selectedCancellation.getSeatNumber(),
            selectedCancellation.getDaysLeft(),
            selectedCancellation.getHoursLeft(),
            selectedCancellation.getBasicAmount(),
            selectedCancellation.getCancelAmount(),
            selectedCancellation.getRefundStatus()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cancellation Details");
        alert.setHeaderText("Detailed Information");
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * Handle process refund
     */
    @FXML
    private void handleProcessRefund() {
        if (selectedCancellation == null) {
            showAlert("Error", "Please select a cancellation to process refund");
            return;
        }

        if (!"Pending".equals(selectedCancellation.getRefundStatus())) {
            showAlert("Error", "Refund has already been processed for this cancellation");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Process Refund");
        confirmation.setHeaderText("Confirm Refund Processing");
        confirmation.setContentText(String.format(
            "Process refund of R %.2f for customer %s?",
            selectedCancellation.getCancelAmount(),
            selectedCancellation.getCustomerCode()
        ));

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Update refund status in database (assuming we add a refund_status column)
                    Connection conn = DatabaseConnection.getConnection();
                    if (conn != null) {
                        // For now, just mark as processed in memory
                        selectedCancellation.setRefundStatus("Processed");
                        cancellationsTable.refresh();

                        showAlert("Success", "Refund processed successfully!");
                        loadStatistics();
                    }

                } catch (Exception e) {
                    logger.severe("Error processing refund: " + e.getMessage());
                    showAlert("Error", "Failed to process refund: " + e.getMessage());
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

    /**
     * Inner class for Cancellation data
     */
    public static class Cancellation {
        private int id;
        private String customerCode;
        private String classType;
        private String seatNumber;
        private int daysLeft;
        private int hoursLeft;
        private double basicAmount;
        private double cancelAmount;
        private String refundStatus;

        public Cancellation() {}

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getCustomerCode() { return customerCode; }
        public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

        public String getClassType() { return classType; }
        public void setClassType(String classType) { this.classType = classType; }

        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

        public int getDaysLeft() { return daysLeft; }
        public void setDaysLeft(int daysLeft) { this.daysLeft = daysLeft; }

        public int getHoursLeft() { return hoursLeft; }
        public void setHoursLeft(int hoursLeft) { this.hoursLeft = hoursLeft; }

        public double getBasicAmount() { return basicAmount; }
        public void setBasicAmount(double basicAmount) { this.basicAmount = basicAmount; }

        public double getCancelAmount() { return cancelAmount; }
        public void setCancelAmount(double cancelAmount) { this.cancelAmount = cancelAmount; }

        public String getRefundStatus() { return refundStatus; }
        public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    }
}
