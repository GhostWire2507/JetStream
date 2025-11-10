package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.models.Flight;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * FlightService handles flight CRUD operations.
 * Uses Statement for queries (per PRD).
 */
public class FlightService {

    public List<Flight> getAllFlights() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT id, flight_number, origin, destination, capacity FROM flights";
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            if (rs != null) {
                while (rs.next()) {
                    Flight f = new Flight(
                            rs.getInt("id"),
                            rs.getString("flight_number"),
                            rs.getString("origin"),
                            rs.getString("destination"),
                            rs.getInt("capacity")
                    );
                    flights.add(f);
                }
            }
        } catch (Exception e) {
            // DatabaseConnection.executeQuery already alerts; optionally log
        }
        return flights;
    }

    public boolean addFlight(Flight f) {
        String sql = String.format(
                "INSERT INTO flights (flight_number, origin, destination, capacity) VALUES ('%s','%s','%s',%d)",
                sanitize(f.getFlightNumber()), sanitize(f.getOrigin()), sanitize(f.getDestination()), f.getCapacity()
        );
        int res = DatabaseConnection.executeUpdate(sql);
        return res > 0;
    }

    public boolean deleteFlight(int id) {
        String sql = "DELETE FROM flights WHERE id=" + id;
        int res = DatabaseConnection.executeUpdate(sql);
        return res > 0;
    }

    public int getTotalFlights() {
        String sql = "SELECT COUNT(*) as total FROM flights";
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            if (rs != null && rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            // Error already logged by DatabaseConnection
        }
        return 0;
    }

    public int getTotalAvailableSeats() {
        String sql = "SELECT SUM(capacity) as total FROM flights";
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            if (rs != null && rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            // Error already logged by DatabaseConnection
        }
        return 0;
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("'", "''"); // basic sanitation to avoid SQL syntax errors
    }
}
