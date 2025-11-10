package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.models.Booking;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    public List<Booking> getBookingsForFlight(int flightId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT id, flight_id, passenger_name, seat_number FROM bookings WHERE flight_id=" + flightId;
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            if (rs != null) {
                while (rs.next()) {
                    Booking b = new Booking(
                            rs.getInt("id"),
                            rs.getInt("flight_id"),
                            rs.getString("passenger_name"),
                            rs.getString("seat_number")
                    );
                    list.add(b);
                }
            }
        } catch (Exception e) {}
        return list;
    }

    public boolean createBooking(Booking booking) {
        String sql = String.format(
                "INSERT INTO bookings (flight_id, passenger_name, seat_number) VALUES (%d, '%s', '%s')",
                booking.getFlightId(), sanitize(booking.getPassengerName()), sanitize(booking.getSeatNumber())
        );
        int res = DatabaseConnection.executeUpdate(sql);
        return res > 0;
    }

    public boolean cancelBooking(int bookingId) {
        String sql = "DELETE FROM bookings WHERE id=" + bookingId;
        int res = DatabaseConnection.executeUpdate(sql);
        return res > 0;
    }

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.id, b.flight_id, b.passenger_name, b.seat_number, f.flight_number " +
                     "FROM bookings b LEFT JOIN flights f ON b.flight_id = f.id";
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            if (rs != null) {
                while (rs.next()) {
                    Booking b = new Booking(
                            rs.getInt("id"),
                            rs.getInt("flight_id"),
                            rs.getString("passenger_name"),
                            rs.getString("seat_number"),
                            rs.getString("flight_number")
                    );
                    list.add(b);
                }
            }
        } catch (Exception e) {
            // Error already logged by DatabaseConnection
        }
        return list;
    }

    public int getTotalBookings() {
        String sql = "SELECT COUNT(*) as total FROM bookings";
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

    public int getTotalPassengers() {
        String sql = "SELECT COUNT(DISTINCT passenger_name) as total FROM bookings";
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

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }
}
