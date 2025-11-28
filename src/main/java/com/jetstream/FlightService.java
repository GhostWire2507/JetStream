package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.database.DualWriteService;
import com.jetstream.models.Flight;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * FlightService handles flight CRUD operations.
 * Uses DualWriteService for write operations to support SQLite + PostgreSQL.
 */
public class FlightService {

    private static final Logger logger = Logger.getLogger(FlightService.class.getName());

    public List<Flight> getAllFlights() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT id, flight_number, origin, destination, 0 as capacity FROM flights ORDER BY departure_time DESC";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.warning("Database not available when fetching flights");
            return flights;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
        } catch (Exception e) {
            logger.severe("Error getting all flights: " + e.getMessage());
        }
        return flights;
    }

    public boolean addFlight(Flight f) {
        String sql = "INSERT INTO flights (flight_number, flight_name, origin, destination, departure_time, arrival_time, travel_date, aircraft_code, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'scheduled')";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String departureTime = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        java.time.LocalDateTime arrival = now.plusHours(2);
        String arrivalTime = arrival.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String travelDate = now.toLocalDate().toString();

        // Use DualWriteService for dual-write to SQLite and PostgreSQL
        int res = DualWriteService.executeUpdatePrepared(sql,
            f.getFlightNumber(), f.getFlightNumber(), f.getOrigin(), f.getDestination(),
            departureTime, arrivalTime, travelDate, "E190");
        if (res < 0) {
            logger.severe("Error adding flight via DualWriteService");
            return false;
        }
        return res > 0;
    }

    public boolean deleteFlight(int id) {
        String sql = "DELETE FROM flights WHERE id=?";
        // Use DualWriteService for dual-write to SQLite and PostgreSQL
        int res = DualWriteService.executeUpdatePrepared(sql, id);
        if (res < 0) {
            logger.severe("Error deleting flight via DualWriteService");
            return false;
        }
        return res > 0;
    }

    public int getTotalFlights() {
        String sql = "SELECT COUNT(*) as total FROM flights";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.warning("Database not available when fetching total flights");
            return 0;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            logger.severe("Error getting total flights: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalAvailableSeats() {
        String sql = "SELECT SUM(COALESCE(club_pre_capacity, 0) + COALESCE(eco_capacity, 0)) as total FROM fleet_information";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.warning("Database not available when fetching total available seats");
            return 0;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            logger.severe("Error getting total available seats: " + e.getMessage());
        }
        return 0;
    }
}
