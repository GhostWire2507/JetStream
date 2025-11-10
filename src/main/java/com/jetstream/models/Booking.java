package com.jetstream.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty; 
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Booking {
    private final IntegerProperty bookingId;
    private int flightId;
    private final StringProperty passengerName;
    private final StringProperty seatNumber;
    private final StringProperty flightNumber;

    public Booking() {
        this.bookingId = new SimpleIntegerProperty(0);
        this.passengerName = new SimpleStringProperty("");
        this.seatNumber = new SimpleStringProperty("");
        this.flightNumber = new SimpleStringProperty("");
    }

    public Booking(int id, int flightId, String passengerName, String seatNumber) {
        this.bookingId = new SimpleIntegerProperty(id);
        this.flightId = flightId;
        this.passengerName = new SimpleStringProperty(passengerName);
        this.seatNumber = new SimpleStringProperty(seatNumber);
        this.flightNumber = new SimpleStringProperty("");
    }

    public Booking(int id, int flightId, String passengerName, String seatNumber, String flightNumber) {
        this.bookingId = new SimpleIntegerProperty(id);
        this.flightId = flightId;
        this.passengerName = new SimpleStringProperty(passengerName);
        this.seatNumber = new SimpleStringProperty(seatNumber);
        this.flightNumber = new SimpleStringProperty(flightNumber);
    }

    // getters / setters
    public int getId() { return bookingId.get(); }
    public void setId(int id) { this.bookingId.set(id); }
    public IntegerProperty bookingIdProperty() { return bookingId; }

    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }

    public String getPassengerName() { return passengerName.get(); }
    public void setPassengerName(String passengerName) { this.passengerName.set(passengerName); }
    public StringProperty passengerNameProperty() { return passengerName; }

    public String getSeatNumber() { return seatNumber.get(); }
    public void setSeatNumber(String seatNumber) { this.seatNumber.set(seatNumber); }
    public StringProperty seatNumberProperty() { return seatNumber; }

    public String getFlightNumber() { return flightNumber.get(); }
    public void setFlightNumber(String flightNumber) { this.flightNumber.set(flightNumber); }
    public StringProperty flightNumberProperty() { return flightNumber; }
}
