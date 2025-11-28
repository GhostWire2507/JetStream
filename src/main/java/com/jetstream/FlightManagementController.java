package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.services.FlightService;
import com.jetstream.utils.SceneManager;
import com.jetstream.models.Flight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Controller for Flight Management screen
 */
public class FlightManagementController {

    private static final Logger logger = Logger.getLogger(FlightManagementController.class.getName());

    @FXML private TextField txtFlightNumber, txtOrigin, txtDestination, txtCapacity;
    @FXML private TextField txtEditFlightNumber, txtEditOrigin, txtEditDestination, txtEditCapacity;
    @FXML private Button btnAddFlight, btnUpdateFlight, btnDeleteFlight;
    @FXML private Label totalFlightsLabel, totalCapacityLabel;
    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, String> colFlightNumber, colOrigin, colDestination;
    @FXML private TableColumn<Flight, Integer> colCapacity, colExecutiveSeats, colEconomySeats;

    private final FlightService flightService = new FlightService();
    private final ObservableList<Flight> flightsList = FXCollections.observableArrayList();
    private Flight selectedFlight;

    @FXML
    public void initialize() {
        logger.info("FlightManagementController initialized");

        // Setup table columns
        colFlightNumber.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        colOrigin.setCellValueFactory(new PropertyValueFactory<>("origin"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colExecutiveSeats.setCellValueFactory(cellData -> {
            Flight flight = cellData.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(getExecutiveSeats(flight)).asObject();
        });
        colEconomySeats.setCellValueFactory(cellData -> {
            Flight flight = cellData.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(getEconomySeats(flight)).asObject();
        });

        // Setup table selection listener
        flightsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateEditFields(newSelection);
                selectedFlight = newSelection;
            }
        });

        // Load data
        loadFlights();
        loadStatistics();
        // Disable admin-only actions for staff users
        try {
            com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
            if (session.isStaff()) {
                btnAddFlight.setDisable(true);
                btnUpdateFlight.setDisable(true);
                btnDeleteFlight.setDisable(true);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Load flights from database
     */
    private void loadFlights() {
        try {
            flightsList.clear();
            // Load flights from database with origin, destination, and capacity from fleet
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "SELECT f.id, f.flight_number, f.flight_name, f.origin, f.destination, f.status, " +
                           "COALESCE(fl.total_capacity, 0) as capacity, " +
                           "COALESCE(fl.club_pre_capacity, 0) as exe_seats, " +
                           "COALESCE(fl.eco_capacity, 0) as eco_seats " +
                           "FROM flights f " +
                           "LEFT JOIN fleet_information fl ON f.aircraft_code = fl.aircraft_code " +
                           "ORDER BY f.departure_time DESC";

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Flight flight = new Flight(
                        rs.getInt("id"),
                        rs.getString("flight_number"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getInt("capacity")
                    );
                    flight.setStatus(rs.getString("status"));
                    flightsList.add(flight);
                }

                rs.close();
                stmt.close();
            }

            flightsTable.setItems(flightsList);
            logger.info("Loaded " + flightsList.size() + " flights");

        } catch (Exception e) {
            logger.severe("Error loading flights: " + e.getMessage());
            showAlert("Error", "Failed to load flights: " + e.getMessage());
        }
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        try {
            totalFlightsLabel.setText(String.valueOf(flightsList.size()));

            int totalCapacity = flightsList.stream()
                .mapToInt(Flight::getCapacity)
                .sum();
            totalCapacityLabel.setText(String.valueOf(totalCapacity));

        } catch (Exception e) {
            logger.warning("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Handle add flight
     */
    @FXML
    private void handleAddFlight() {
        if (!validateFlightInput(txtFlightNumber, txtOrigin, txtDestination, txtCapacity)) {
            return;
        }

        try {
            Flight flight = new Flight();
            flight.setFlightNumber(txtFlightNumber.getText().trim());
            flight.setOrigin(txtOrigin.getText().trim());
            flight.setDestination(txtDestination.getText().trim());
            flight.setCapacity(Integer.parseInt(txtCapacity.getText().trim()));

            boolean success = flightService.addFlight(flight);
            if (success) {
                showAlert("Success", "Flight added successfully!");
                clearAddFields();
                loadFlights();
                loadStatistics();
            } else {
                showAlert("Error", "Failed to add flight");
            }

        } catch (Exception e) {
            logger.severe("Error adding flight: " + e.getMessage());
            showAlert("Error", "Failed to add flight: " + e.getMessage());
        }
    }

    /**
     * Handle update flight
     */
    @FXML
    private void handleUpdateFlight() {
        if (selectedFlight == null) {
            showAlert("Error", "Please select a flight to update");
            return;
        }

        if (!validateFlightInput(txtEditFlightNumber, txtEditOrigin, txtEditDestination, txtEditCapacity)) {
            return;
        }

        try {
            // Update flight in database
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "UPDATE flights SET flight_name = ? WHERE id = ?";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, txtEditFlightNumber.getText().trim());
                stmt.setInt(2, selectedFlight.getId());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Flight updated successfully!");
                    loadFlights();
                    loadStatistics();
                } else {
                    showAlert("Error", "Failed to update flight");
                }

                stmt.close();
            }

        } catch (Exception e) {
            logger.severe("Error updating flight: " + e.getMessage());
            showAlert("Error", "Failed to update flight: " + e.getMessage());
        }
    }

    /**
     * Handle delete flight
     */
    @FXML
    private void handleDeleteFlight() {
        if (selectedFlight == null) {
            showAlert("Error", "Please select a flight to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete Flight");
        confirmation.setContentText("Are you sure you want to delete flight " + selectedFlight.getFlightNumber() + "?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = flightService.deleteFlight(selectedFlight.getId());
                    if (success) {
                        showAlert("Success", "Flight deleted successfully!");
                        loadFlights();
                        loadStatistics();
                        clearEditFields();
                        selectedFlight = null;
                    } else {
                        showAlert("Error", "Failed to delete flight");
                    }

                } catch (Exception e) {
                    logger.severe("Error deleting flight: " + e.getMessage());
                    showAlert("Error", "Failed to delete flight: " + e.getMessage());
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
     * Populate edit fields with selected flight data
     */
    private void populateEditFields(Flight flight) {
        txtEditFlightNumber.setText(flight.getFlightNumber());
        txtEditOrigin.setText(flight.getOrigin());
        txtEditDestination.setText(flight.getDestination());
        txtEditCapacity.setText(String.valueOf(flight.getCapacity()));
    }

    /**
     * Clear add flight fields
     */
    private void clearAddFields() {
        txtFlightNumber.clear();
        txtOrigin.clear();
        txtDestination.clear();
        txtCapacity.clear();
    }

    /**
     * Clear edit flight fields
     */
    private void clearEditFields() {
        txtEditFlightNumber.clear();
        txtEditOrigin.clear();
        txtEditDestination.clear();
        txtEditCapacity.clear();
    }

    /**
     * Validate flight input fields
     */
    private boolean validateFlightInput(TextField flightNumber, TextField origin, TextField destination, TextField capacity) {
        if (flightNumber.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Flight number is required");
            return false;
        }

        if (capacity.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Capacity is required");
            return false;
        }

        try {
            int cap = Integer.parseInt(capacity.getText().trim());
            if (cap <= 0) {
                showAlert("Validation Error", "Capacity must be a positive number");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Capacity must be a valid number");
            return false;
        }

        return true;
    }

    /**
     * Get executive seats for a flight (assuming 30% executive, 70% economy)
     */
    private int getExecutiveSeats(Flight flight) {
        return (int) Math.round(flight.getCapacity() * 0.3);
    }

    /**
     * Get economy seats for a flight
     */
    private int getEconomySeats(Flight flight) {
        return flight.getCapacity() - getExecutiveSeats(flight);
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
