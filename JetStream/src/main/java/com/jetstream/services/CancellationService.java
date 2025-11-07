package com.jetstream.services;

public class CancellationService {

    private final BookingService bookingService = new BookingService();

    public boolean cancelBooking(int bookingId) {

        return bookingService.cancelBooking(bookingId);
    }
}
