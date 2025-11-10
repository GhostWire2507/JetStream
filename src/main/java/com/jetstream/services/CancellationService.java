package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;

import java.sql.ResultSet;

public class CancellationService {

    private final BookingService bookingService = new BookingService();

    public boolean cancelBooking(int bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    public int getTotalCancellations() {
        // Note: This assumes you have a cancellations table
        // If not, this will return 0 and you'll need to create the table
        String sql = "SELECT COUNT(*) as total FROM cancellations";
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            if (rs != null && rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            // If table doesn't exist, return 0
            // You may want to create a cancellations table or track this differently
        }
        return 0;
    }
}
