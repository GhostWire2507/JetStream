package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.database.DualWriteService;
import com.jetstream.manager.SessionManager;
import com.jetstream.models.Booking;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BookingService {

    private static final Logger logger = Logger.getLogger(BookingService.class.getName());

    public List<Booking> getBookingsForFlight(int flightId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.id, b.flight_id, COALESCE(cd.cust_name, '') as passenger_name, b.seat_id as seat_number FROM bookings b LEFT JOIN customer_details cd ON b.customer_id = cd.id WHERE b.flight_id=?";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.warning("Database not available when fetching bookings for flight");
            return list;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flightId);
            try (ResultSet rs = stmt.executeQuery()) {
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
        } catch (Exception e) {
            logger.severe("Error getting bookings for flight: " + e.getMessage());
        }
        return list;
    }

    public boolean createBooking(Booking booking) {
        // Generate PNR number if not provided
        if (booking.getPnrNumber() == null || booking.getPnrNumber().isEmpty()) {
            booking.setPnrNumber("PNR" + System.currentTimeMillis() + (int)(Math.random() * 1000));
        }

        String sql = "INSERT INTO bookings (pnr_number, customer_id, flight_id, seat_id, booking_date, travel_date, class_code, status, base_amount, final_amount) VALUES (?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_DATE, 'ECO', 'confirmed', 100.0, 100.0)";
        // Use DualWriteService for dual-write to SQLite and PostgreSQL
        int res = DualWriteService.executeUpdatePrepared(sql, booking.getPnrNumber(), booking.getCustomerId(), booking.getFlightId());
        if (res < 0) {
            logger.severe("Error creating booking via DualWriteService");
            return false;
        }
        return res > 0;
    }

    public boolean cancelBooking(int bookingId) {
        String sql = "UPDATE bookings SET status = 'cancelled' WHERE id=?";
        // Use DualWriteService for dual-write to SQLite and PostgreSQL
        int res = DualWriteService.executeUpdatePrepared(sql, bookingId);
        if (res < 0) {
            logger.severe("Error canceling booking via DualWriteService");
            return false;
        }
        return res > 0;
    }

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.id, b.pnr_number, b.flight_id, b.seat_id, b.booking_date, b.travel_date, b.class_code, b.status, b.base_amount, b.final_amount, f.flight_number, cd.cust_name as passenger_name, s.seat_number " +
                     "FROM bookings b " +
                     "LEFT JOIN flights f ON b.flight_id = f.id " +
                     "LEFT JOIN customer_details cd ON b.customer_id = cd.id " +
                     "LEFT JOIN seats s ON b.seat_id = s.id " +
                     "ORDER BY b.booking_date DESC";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.warning("Database not available when fetching all bookings");
            return list;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Booking b = new Booking();
                b.setId(rs.getInt("id"));
                b.setPnrNumber(rs.getString("pnr_number"));
                b.setFlightId(rs.getInt("flight_id"));
                b.setPassengerName(rs.getString("passenger_name"));
                b.setFlightNumber(rs.getString("flight_number"));
                b.setSeatNumber(rs.getString("seat_number"));
                b.setStatus(rs.getString("status"));
                b.setBookingDate(rs.getString("booking_date"));
                b.setAmount(rs.getDouble("final_amount"));
                list.add(b);
            }
        } catch (Exception e) {
            logger.severe("Error getting all bookings: " + e.getMessage());
        }
        return list;
    }

    public int getTotalBookings() {
        try {
            String userEmail = SessionManager.getInstance().getEmail();
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warning("No user session found for getTotalBookings");
                return 0;
            }
            String sql = "SELECT COUNT(*) as total FROM bookings b " +
                        "WHERE b.customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?))";
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) {
                logger.warning("Database not available when fetching total bookings");
                return 0;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userEmail);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting total bookings: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalPassengers() {
        try {
            String userEmail = SessionManager.getInstance().getEmail();
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warning("No user session found for getTotalPassengers");
                return 0;
            }
            String sql = "SELECT COUNT(DISTINCT b.customer_id) as total FROM bookings b " +
                        "WHERE b.customer_id IN (SELECT id FROM customer_details WHERE user_id = (SELECT id FROM users WHERE email = ?))";
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) {
                logger.warning("Database not available when fetching total passengers");
                return 0;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userEmail);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting total passengers: " + e.getMessage());
        }
        return 0;
    }
}
