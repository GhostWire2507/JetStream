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
        System.out.println("MainController.initialize() called");
        try {
            loadDashboardMetrics();
            applyCardAnimations();
            System.out.println("✓ MainController initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ Error in MainController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDashboardMetrics() {
        try {
            System.out.println("Loading dashboard metrics...");
            int flights = flightService.getTotalFlights();
            int bookings = bookingService.getTotalBookings();
            int cancellations = cancellationService.getTotalCancellations();
            int passengers = bookingService.getTotalPassengers();

            txtTotalFlights.setText(String.valueOf(flights));
            txtTotalBookings.setText(String.valueOf(bookings));
            txtTotalCancellations.setText(String.valueOf(cancellations));
            txtTotalPassengers.setText(String.valueOf(passengers));

            System.out.println("✓ Dashboard metrics loaded: Flights=" + flights + ", Bookings=" + bookings +
                             ", Cancellations=" + cancellations + ", Passengers=" + passengers);
        } catch (Exception e) {
            System.err.println("✗ Failed to load dashboard metrics: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load dashboard metrics: " + e.getMessage());
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
