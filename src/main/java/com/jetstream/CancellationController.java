package com.jetstream.controllers;

import com.jetstream.models.Booking;
import com.jetstream.services.BookingService;
import com.jetstream.services.CancellationService;
import com.jetstream.services.FlightService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class CancellationController extends BaseController {

    @FXML private TextField txtBookingId;
    @FXML private Button btnCancel, btnBack;
    @FXML private Text txtTotalCancellations, txtAvailableSeats;
    @FXML private TableView<Booking> tblReservations;
    @FXML private TableColumn<Booking, Integer> colBookingId;
    @FXML private TableColumn<Booking, String> colPassengerName, colSeatNumber, colFlightNumber;

    private final CancellationService cancellationService = new CancellationService();
    private final BookingService bookingService = new BookingService();
    private final FlightService flightService = new FlightService();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Bind TableColumns
        colBookingId.setCellValueFactory(b -> b.getValue().bookingIdProperty().asObject());
        colPassengerName.setCellValueFactory(b -> b.getValue().passengerNameProperty());
        colSeatNumber.setCellValueFactory(b -> b.getValue().seatNumberProperty());
        colFlightNumber.setCellValueFactory(b -> b.getValue().flightNumberProperty());

        loadReservations();
        loadDashboardMetrics();
        applyCardAnimations();
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
            txtTotalCancellations.setText(String.valueOf(cancellationService.getTotalCancellations()));
            txtAvailableSeats.setText(String.valueOf(flightService.getTotalAvailableSeats()));
        } catch (Exception e) {
            showAlert("Error", "Failed to load metrics.");
        }
    }

    private void applyCardAnimations() {
        for (Text cardValue : new Text[]{txtTotalCancellations, txtAvailableSeats}) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1), cardValue);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    @FXML
    private void onCancel() {
        try {
            int id = Integer.parseInt(txtBookingId.getText());
            boolean ok = cancellationService.cancelBooking(id);
            if (ok) {
                showAlert("Success", "Booking cancelled.");
                loadReservations();
                loadDashboardMetrics();
            } else {
                showAlert("Failure", "Cancellation failed.");
            }
        } catch (NumberFormatException e) {
            showAlert("Validation", "Enter a valid booking ID.");
        }
    }

    @FXML
    private void onBack() { goTo("customer_dashboard.fxml"); }
}
