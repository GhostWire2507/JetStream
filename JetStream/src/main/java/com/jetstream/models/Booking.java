package com.jetstream.models;

public class Booking {
    private int id;
    private int flightId;
    private String passengerName;
    private String seatNumber;

    public Booking() {}

    public Booking(int id, int flightId, String passengerName, String seatNumber) {
        this.id = id;
        this.flightId = flightId;
        this.passengerName = passengerName;
        this.seatNumber = seatNumber;
    }

    // getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
}
