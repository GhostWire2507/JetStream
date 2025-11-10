package com.jetstream.controllers;

import com.jetstream.models.Booking;
import com.jetstream.services.BookingService;
import com.jetstream.services.FlightService;
import com.jetstream.utils.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class ReservationController extends BaseController {

    @FXML private TextField txtPassengerName, txtSeatNumber;
    @FXML private Button btnBook, btnBack;
    @FXML private Text txtTotalBookings, txtTotalPassengers;
    @FXML private TableView<Booking> tblReservations;
    @FXML private TableColumn<Booking, String> colPassengerName, colSeatNumber, colFlightNumber;
    @FXML private TableColumn<Booking, Integer> colBookingId;

    private final BookingService bookingService = new BookingService();
    private final FlightService flightService = new FlightService();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Bind TableColumns
        colPassengerName.setCellValueFactory(b -> b.getValue().passengerNameProperty());
        colSeatNumber.setCellValueFactory(b -> b.getValue().seatNumberProperty());
        colFlightNumber.setCellValueFactory(b -> b.getValue().flightNumberProperty());
        colBookingId.setCellValueFactory(b -> b.getValue().bookingIdProperty().asObject());

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
            txtTotalBookings.setText(String.valueOf(bookingService.getTotalBookings()));
            txtTotalPassengers.setText(String.valueOf(bookingService.getTotalPassengers()));
        } catch (Exception e) {
            showAlert("Error", "Failed to load metrics.");
        }
    }

    private void applyCardAnimations() {
        for (Text cardValue : new Text[]{txtTotalBookings, txtTotalPassengers}) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1), cardValue);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    @FXML
    private void onBook() {
        String name = txtPassengerName.getText();
        String seat = txtSeatNumber.getText();

        if (!Validator.isNotEmpty(name) || !Validator.isNotEmpty(seat)) {
            showAlert("Validation", "Please enter passenger name and seat number.");
            return;
        }

        // For demo: assume flight ID = 1
        Booking booking = new Booking();
        booking.setFlightId(1);
        booking.setPassengerName(name);
        booking.setSeatNumber(seat);

        boolean ok = bookingService.createBooking(booking);
        if (ok) {
            showAlert("Success", "Booking created.");
            loadReservations();
            loadDashboardMetrics();
        } else {
            showAlert("Failure", "Could not create booking.");
        }
    }

    @FXML
    private void onBack() { goTo("main.fxml"); }
}
