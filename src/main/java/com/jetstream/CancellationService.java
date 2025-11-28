package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class CancellationService {

    private static final Logger logger = Logger.getLogger(CancellationService.class.getName());
    private final BookingService bookingService = new BookingService();

    public boolean cancelBooking(int bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    public int getTotalCancellations() {
        // Use the cancellation table from schema
        String sql = "SELECT COUNT(*) as total FROM cancellations";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.warning("Database not available when fetching total cancellations");
            return 0;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            logger.severe("Error getting total cancellations: " + e.getMessage());
        }
        return 0;
    }
}
