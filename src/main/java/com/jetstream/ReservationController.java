package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.manager.SessionManager;
import com.jetstream.models.Booking;
import com.jetstream.models.Flight;
import com.jetstream.services.BookingService;
import com.jetstream.services.FlightService;
import com.jetstream.utils.SceneManager;
import com.jetstream.utils.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ReservationController extends BaseController {

    private static final Logger logger = Logger.getLogger(ReservationController.class.getName());

    // Step panes
    @FXML private VBox step1Pane, step2Pane, step3Pane, step4Pane, step5Pane;

    // Step labels
    @FXML private Label step1Label, step2Label, step3Label, step4Label, step5Label;

    // Form fields
    @FXML private TextField txtPassengerName, txtSeatNumber, txtPaymentAmount;
    @FXML private ComboBox<Flight> flightComboBox;
    @FXML private TextField txtPassengerEmail;
    @FXML private Label confirmationDetails;

    // Navigation buttons
    @FXML private Button btnPrevious, btnNext, btnContinueStep1;

    // Progress
    @FXML private ProgressBar bookingProgressBar;
    @FXML private Label progressLabel, stepCounter;

    // Statistics
    @FXML private Label txtTotalBookings, txtTotalPassengers;

    // Recent reservations table
    @FXML private TableView<Booking> tblReservations;
    @FXML private TableColumn<Booking, String> colPassengerName, colSeatNumber, colFlightNumber;
    @FXML private TableColumn<Booking, Integer> colBookingId;

    // Welcome label
    @FXML private Label welcomeLabel;

    private final BookingService bookingService = new BookingService();
    private final FlightService flightService = new FlightService();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();
    private final ObservableList<Flight> flightList = FXCollections.observableArrayList();

    private int currentStep = 1;
    private Booking currentBooking = new Booking();

    @FXML
    public void initialize() {
        // Welcome message
        String fullName = SessionManager.getInstance().getFullName();
        welcomeLabel.setText("Welcome, " + (fullName != null && !fullName.isEmpty() ? fullName :
                SessionManager.getInstance().getUsername()));

        // Flight combo box
        flightComboBox.setItems(flightList);
        flightComboBox.setConverter(new javafx.util.StringConverter<Flight>() {
            @Override
            public String toString(Flight flight) {
                return flight != null ? flight.getFlightNumber() + " - " + flight.getOrigin() + " to " + flight.getDestination() : "";
            }
            @Override
            public Flight fromString(String string) { return null; }
        });

        // Flight selection listener
        flightComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            btnNext.setDisable(newVal == null);
            btnContinueStep1.setDisable(newVal == null);
        });

        // Table bindings
        colPassengerName.setCellValueFactory(b -> b.getValue().passengerNameProperty());
        colSeatNumber.setCellValueFactory(b -> b.getValue().seatNumberProperty());
        colFlightNumber.setCellValueFactory(b -> b.getValue().flightNumberProperty());
        colBookingId.setCellValueFactory(b -> b.getValue().bookingIdProperty().asObject());

        // Initialize booking
        currentBooking = new Booking();
        String userEmail = SessionManager.getInstance().getEmail();
        String userName = SessionManager.getInstance().getFullName();
        currentBooking.setPassengerEmail(userEmail != null ? userEmail : "");
        currentBooking.setPassengerName(userName != null && !userName.isEmpty() ? userName :
                SessionManager.getInstance().getUsername());

        // Set customer ID from customer_details table
        int userId = SessionManager.getInstance().getUserId();
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "SELECT id FROM customer_details WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        currentBooking.setCustomerId(rs.getInt("id"));
                    } else {
                        // If no customer_details record exists, use user_id as fallback
                        currentBooking.setCustomerId(userId);
                        logger.warning("No customer_details record found for user_id: " + userId);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Error fetching customer ID: " + e.getMessage());
            currentBooking.setCustomerId(userId);
        }

        // Load data
        loadFlights();
        loadReservations();
        loadDashboardMetrics();

        // Step visibility & progress
        updateStepVisibility();
        updateProgress();

        // Ensure Next button state after layout
        Platform.runLater(() -> btnNext.setDisable(flightComboBox.getValue() == null));
    }

    private void loadFlights() {
        try {
            List<Flight> flights = flightService.getAllFlights();
            flightList.setAll(flights);

            // Auto-select first flight if available
            if (!flightList.isEmpty() && flightComboBox.getValue() == null) {
                flightComboBox.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load flights: " + e.getMessage());
        }
    }

    private void loadReservations() {
        try {
            bookingList.setAll(bookingService.getAllBookings());
            tblReservations.setItems(bookingList);
        } catch (Exception e) {
            showAlert("Error", "Failed to load reservations.");
        }
    }

    private void loadDashboardMetrics() {
        try {
            txtTotalBookings.setText(String.valueOf(bookingService.getTotalBookings()));
            txtTotalPassengers.setText(String.valueOf(bookingService.getTotalPassengers()));
        } catch (Exception e) {
            showAlert("Error", "Failed to load metrics.");
        }
    }

    private void updateStepVisibility() {
        step1Pane.setVisible(currentStep == 1);
        step2Pane.setVisible(currentStep == 2);
        step3Pane.setVisible(currentStep == 3);
        step4Pane.setVisible(currentStep == 4);
        step5Pane.setVisible(currentStep == 5);

        step1Label.setStyle(currentStep == 1 ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;" : "-fx-text-fill: #666;");
        step2Label.setStyle(currentStep == 2 ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;" : "-fx-text-fill: #666;");
        step3Label.setStyle(currentStep == 3 ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;" : "-fx-text-fill: #666;");
        step4Label.setStyle(currentStep == 4 ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;" : "-fx-text-fill: #666;");
        step5Label.setStyle(currentStep == 5 ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;" : "-fx-text-fill: #666;");

        btnPrevious.setDisable(currentStep == 1);
        btnNext.setText(currentStep == 5 ? "Confirm Booking" : "Next: " + getNextStepName());
    }

    private String getNextStepName() {
        switch (currentStep) {
            case 1: return "Passenger Details";
            case 2: return "Seat Selection";
            case 3: return "Payment";
            case 4: return "Confirmation";
            default: return "Next";
        }
    }

    private void updateProgress() {
        double progress = (currentStep - 1) / 4.0;
        bookingProgressBar.setProgress(progress);
        String[] stepNames = {"Flight Selection", "Passenger Details", "Seat Selection", "Payment", "Confirmation"};
        progressLabel.setText(String.format("%.0f%% Complete - %s", progress * 100, stepNames[currentStep - 1]));
        stepCounter.setText(currentStep + " of 5");
    }

    @FXML
    private void onNext() {
        if (currentStep < 5) {
            if (validateCurrentStep()) {
                saveCurrentStepData();
                currentStep++;
                updateStepVisibility();
                updateProgress();
                loadStepData();
            }
        } else if (confirmBooking()) {
            showAlert("Success", "Booking confirmed successfully!");
            SceneManager.getInstance().loadCustomerDashboard();
        }
    }

    @FXML
    private void onContinueStep1() {
        // Continue button on Step 1 - directly proceed to Step 2
        if (flightComboBox.getValue() != null) {
            saveCurrentStepData();
            currentStep++;
            updateStepVisibility();
            updateProgress();
            loadStepData();
        }
    }

    @FXML
    private void onPrevious() {
        if (currentStep > 1) {
            currentStep--;
            updateStepVisibility();
            updateProgress();
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1: return flightComboBox.getValue() != null || showValidationAlert("Please select a flight.");
            case 2:
                if (!Validator.isNotEmpty(txtPassengerName.getText()) || !Validator.isNotEmpty(txtPassengerEmail.getText()))
                    return showValidationAlert("Please enter passenger name and email.");
                if (!Validator.isValidEmail(txtPassengerEmail.getText()))
                    return showValidationAlert("Please enter a valid email address.");
                return true;
            case 3:
                return Validator.isNotEmpty(txtSeatNumber.getText()) || showValidationAlert("Please enter a seat number.");
            case 4:
                // Validate payment amount
                if (!Validator.isNotEmpty(txtPaymentAmount.getText()))
                    return showValidationAlert("Please enter a payment amount.");
                try {
                    double amount = Double.parseDouble(txtPaymentAmount.getText());
                    if (amount <= 0)
                        return showValidationAlert("Payment amount must be greater than 0.");
                    return true;
                } catch (NumberFormatException e) {
                    return showValidationAlert("Please enter a valid numeric amount.");
                }
            default: return true;
        }
    }

    private boolean showValidationAlert(String msg) {
        showAlert("Validation", msg);
        return false;
    }

    private void saveCurrentStepData() {
        switch (currentStep) {
            case 1:
                Flight selectedFlight = flightComboBox.getValue();
                if (selectedFlight != null) {
                    currentBooking.setFlightId(selectedFlight.getId());
                    currentBooking.setFlightNumber(selectedFlight.getFlightNumber());
                }
                break;
            case 2:
                currentBooking.setPassengerName(txtPassengerName.getText());
                currentBooking.setPassengerEmail(txtPassengerEmail.getText());
                break;
            case 3:
                currentBooking.setSeatNumber(txtSeatNumber.getText());
                break;
            case 4:
                // Save payment amount
                try {
                    double amount = Double.parseDouble(txtPaymentAmount.getText());
                    currentBooking.setAmount(amount);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid payment amount entered: " + e.getMessage());
                }
                break;
        }
    }

    private void loadStepData() {
        switch (currentStep) {
            case 2:
                txtPassengerName.setText(currentBooking.getPassengerName());
                txtPassengerEmail.setText(currentBooking.getPassengerEmail());
                break;
            case 3:
                txtSeatNumber.setText(currentBooking.getSeatNumber());
                break;
            case 4:
                // Load saved amount if exists
                if (currentBooking.getAmount() > 0) {
                    txtPaymentAmount.setText(String.format("%.2f", currentBooking.getAmount()));
                }
                break;
            case 5:
                updateConfirmationDetails();
                break;
        }
    }

    private void updateConfirmationDetails() {
        confirmationDetails.setText(String.format(
                "Flight: %s\nPassenger: %s\nEmail: %s\nSeat: %s\nAmount: R %.2f",
                currentBooking.getFlightNumber(),
                currentBooking.getPassengerName(),
                currentBooking.getPassengerEmail(),
                currentBooking.getSeatNumber(),
                currentBooking.getAmount()
        ));
    }

    private boolean confirmBooking() {
        try {
            // Use the BookingService to create the booking
            boolean success = bookingService.createBooking(currentBooking);

            if (success) {
                // Refresh the local UI data
                loadReservations();
                loadDashboardMetrics();

                // Show success message with PNR
                showAlert("Success", "Booking confirmed successfully!\nPNR: " + currentBooking.getPnrNumber());
                return true;
            } else {
                showAlert("Error", "Failed to create booking. Please try again.");
                return false;
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to create booking: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void onBack() {
        SceneManager.getInstance().loadCustomerDashboard();
    }

    protected void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
