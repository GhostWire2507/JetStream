package com.jetstream.controllers;

import com.jetstream.services.BookingService;
import com.jetstream.services.CancellationService;
import com.jetstream.services.FlightService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class MainController extends BaseController {

    @FXML private Text txtTotalFlights;
    @FXML private Text txtTotalBookings;
    @FXML private Text txtTotalCancellations;
    @FXML private Text txtTotalPassengers;

    private final FlightService flightService = new FlightService();
    private final BookingService bookingService = new BookingService();
    private final CancellationService cancellationService = new CancellationService();

    @FXML
    public void initialize() {
        loadDashboardMetrics();
        applyCardAnimations();
    }

    private void loadDashboardMetrics() {
        try {
            txtTotalFlights.setText(String.valueOf(flightService.getTotalFlights()));
            txtTotalBookings.setText(String.valueOf(bookingService.getTotalBookings()));
            txtTotalCancellations.setText(String.valueOf(cancellationService.getTotalCancellations()));
            txtTotalPassengers.setText(String.valueOf(bookingService.getTotalPassengers()));
        } catch (Exception e) {
            showAlert("Error", "Failed to load dashboard metrics.");
        }
    }

    private void applyCardAnimations() {
        FadeTransition ft1 = new FadeTransition(Duration.seconds(1), txtTotalFlights);
        ft1.setFromValue(0);
        ft1.setToValue(1);
        ft1.play();

        FadeTransition ft2 = new FadeTransition(Duration.seconds(1), txtTotalBookings);
        ft2.setFromValue(0);
        ft2.setToValue(1);
        ft2.play();

        FadeTransition ft3 = new FadeTransition(Duration.seconds(1), txtTotalCancellations);
        ft3.setFromValue(0);
        ft3.setToValue(1);
        ft3.play();

        FadeTransition ft4 = new FadeTransition(Duration.seconds(1), txtTotalPassengers);
        ft4.setFromValue(0);
        ft4.setToValue(1);
        ft4.play();
    }

    @FXML private void onReservation() { goTo("reservation.fxml"); }
    @FXML private void onFlight() { goTo("flight.fxml"); }
    @FXML private void onCancellation() { goTo("cancellation.fxml"); }
    @FXML private void onAdmin() { goTo("admin.fxml"); }
}
