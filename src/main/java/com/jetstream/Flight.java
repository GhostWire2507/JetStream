package com.jetstream.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Flight model
 */
public class Flight {
    private int id;
    private final StringProperty flightNumber;
    private final StringProperty origin;
    private final StringProperty destination;
    private final IntegerProperty capacity;
    private final StringProperty status;

    public Flight(int id, String flightNumber, String origin, String destination, int capacity) {
        this.id = id;
        this.flightNumber = new SimpleStringProperty(flightNumber);
        this.origin = new SimpleStringProperty(origin);
        this.destination = new SimpleStringProperty(destination);
        this.capacity = new SimpleIntegerProperty(capacity);
        this.status = new SimpleStringProperty("scheduled");
    }

    public Flight() {
        this.flightNumber = new SimpleStringProperty("");
        this.origin = new SimpleStringProperty("");
        this.destination = new SimpleStringProperty("");
        this.capacity = new SimpleIntegerProperty(0);
        this.status = new SimpleStringProperty("scheduled");
    }

    // getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFlightNumber() { return flightNumber.get(); }
    public void setFlightNumber(String flightNumber) { this.flightNumber.set(flightNumber); }
    public StringProperty flightNumberProperty() { return flightNumber; }

    public String getOrigin() { return origin.get(); }
    public void setOrigin(String origin) { this.origin.set(origin); }
    public StringProperty originProperty() { return origin; }

    public String getDestination() { return destination.get(); }
    public void setDestination(String destination) { this.destination.set(destination); }
    public StringProperty destinationProperty() { return destination; }

    public int getCapacity() { return capacity.get(); }
    public void setCapacity(int capacity) { this.capacity.set(capacity); }
    public IntegerProperty capacityProperty() { return capacity; }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }
}
