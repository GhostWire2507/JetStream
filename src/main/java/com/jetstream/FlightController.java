package com.jetstream.controllers;

import com.jetstream.models.Flight;
import com.jetstream.services.FlightService;
import com.jetstream.utils.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class FlightController extends BaseController {

    @FXML private TextField txtFlightNumber, txtOrigin, txtDestination, txtCapacity;
    @FXML private Button btnAddFlight, btnBack;
    @FXML private Text txtTotalFlights, txtAvailableSeats;
    @FXML private TableView<Flight> tblFlights;
    @FXML private TableColumn<Flight, String> colFlightNumber, colOrigin, colDestination;
    @FXML private TableColumn<Flight, Integer> colCapacity;

    private final FlightService flightService = new FlightService();
    private final ObservableList<Flight> flightList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Bind TableColumns
        colFlightNumber.setCellValueFactory(f -> f.getValue().flightNumberProperty());
        colOrigin.setCellValueFactory(f -> f.getValue().originProperty());
        colDestination.setCellValueFactory(f -> f.getValue().destinationProperty());
        colCapacity.setCellValueFactory(f -> f.getValue().capacityProperty().asObject());

        loadFlights();
        loadDashboardMetrics();
        applyCardAnimations();
    }

    private void loadFlights() {
        try {
            flightList.setAll(flightService.getAllFlights());
            tblFlights.setItems(flightList);
        } catch (Exception e) {
            showAlert("Error", "Failed to load flights.");
        }
    }

    private void loadDashboardMetrics() {
        try {
            txtTotalFlights.setText(String.valueOf(flightService.getTotalFlights()));
            txtAvailableSeats.setText(String.valueOf(flightService.getTotalAvailableSeats()));
        } catch (Exception e) {
            showAlert("Error", "Failed to load metrics.");
        }
    }

    private void applyCardAnimations() {
        for (Text cardValue : new Text[]{txtTotalFlights, txtAvailableSeats}) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1), cardValue);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    @FXML
    private void onAddFlight() {
        if (!Validator.isNotEmpty(txtFlightNumber.getText())) {
            showAlert("Validation", "Flight name is required.");
            return;
        }
        if (!Validator.isInteger(txtCapacity.getText())) {
            showAlert("Validation", "Capacity must be a number.");
            return;
        }

        Flight f = new Flight();
        f.setFlightNumber(txtFlightNumber.getText());
        f.setOrigin(txtOrigin.getText()); // Not used in schema but kept for compatibility
        f.setDestination(txtDestination.getText()); // Not used in schema but kept for compatibility
        f.setCapacity(Integer.parseInt(txtCapacity.getText()));

        boolean ok = flightService.addFlight(f);
        if (ok) {
            showAlert("Success", "Flight added.");
            loadFlights();
            loadDashboardMetrics();
        } else {
            showAlert("Failure", "Could not add flight.");
        }
    }

    @FXML
    private void onBack() {
        // Route to the new Admin Dashboard instead of legacy main.fxml
        goTo("admin_dashboard.fxml");
    }
}
