package com.jetstream.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Booking {
    private final IntegerProperty bookingId;
    private int flightId;
    private int customerId;
    private final StringProperty passengerName;
    private final StringProperty passengerEmail;
    private final StringProperty seatNumber;
    private final StringProperty flightNumber;
    private final StringProperty pnrNumber;
    private final StringProperty status;
    private final StringProperty bookingDate;
    private double amount;
    private final StringProperty travelDate;
    private final StringProperty classCode;

    public Booking() {
        this.bookingId = new SimpleIntegerProperty(0);
        this.passengerName = new SimpleStringProperty("");
        this.passengerEmail = new SimpleStringProperty("");
        this.seatNumber = new SimpleStringProperty("");
        this.flightNumber = new SimpleStringProperty("");
        this.pnrNumber = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("pending");
        this.bookingDate = new SimpleStringProperty("");
        this.travelDate = new SimpleStringProperty("");
        this.classCode = new SimpleStringProperty("");
    }

    public Booking(int id, int flightId, String passengerName, String seatNumber) {
        this.bookingId = new SimpleIntegerProperty(id);
        this.flightId = flightId;
        this.passengerName = new SimpleStringProperty(passengerName);
        this.passengerEmail = new SimpleStringProperty("");
        this.seatNumber = new SimpleStringProperty(seatNumber);
            this.travelDate = new SimpleStringProperty("");
            this.classCode = new SimpleStringProperty("");
        this.flightNumber = new SimpleStringProperty("");
        this.pnrNumber = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("pending");
        this.bookingDate = new SimpleStringProperty("");
    }

    public Booking(int id, int flightId, String passengerName, String seatNumber, String flightNumber) {
        this.bookingId = new SimpleIntegerProperty(id);
        this.flightId = flightId;
        this.passengerName = new SimpleStringProperty(passengerName);
        this.passengerEmail = new SimpleStringProperty("");
        this.seatNumber = new SimpleStringProperty(seatNumber);
        this.flightNumber = new SimpleStringProperty(flightNumber);
            this.travelDate = new SimpleStringProperty("");
            this.classCode = new SimpleStringProperty("");
        this.pnrNumber = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("pending");
        this.bookingDate = new SimpleStringProperty("");
    }

    // getters / setters
    public int getId() { return bookingId.get(); }
    public void setId(int id) { this.bookingId.set(id); }
    public IntegerProperty bookingIdProperty() { return bookingId; }

    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getPassengerName() { return passengerName.get(); }
    public void setPassengerName(String passengerName) { this.passengerName.set(passengerName); }
    public StringProperty passengerNameProperty() { return passengerName; }

    public String getPassengerEmail() { return passengerEmail.get(); }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail.set(passengerEmail); }
    public StringProperty passengerEmailProperty() { return passengerEmail; }

    public String getSeatNumber() { return seatNumber.get(); }
    public void setSeatNumber(String seatNumber) { this.seatNumber.set(seatNumber); }
    public StringProperty seatNumberProperty() { return seatNumber; }

    public String getFlightNumber() { return flightNumber.get(); }
    public void setFlightNumber(String flightNumber) { this.flightNumber.set(flightNumber); }
    public StringProperty flightNumberProperty() { return flightNumber; }

    public String getPnrNumber() { return pnrNumber.get(); }
    public void setPnrNumber(String pnrNumber) { this.pnrNumber.set(pnrNumber); }
    public StringProperty pnrNumberProperty() { return pnrNumber; }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }

    public String getBookingDate() { return bookingDate.get(); }
    public void setBookingDate(String bookingDate) { this.bookingDate.set(bookingDate); }
    public StringProperty bookingDateProperty() { return bookingDate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTravelDate() { return travelDate.get(); }
    public void setTravelDate(String travelDate) { this.travelDate.set(travelDate); }
    public StringProperty travelDateProperty() { return travelDate; }

    public String getClassCode() { return classCode.get(); }
    public void setClassCode(String classCode) { this.classCode.set(classCode); }
    public StringProperty classCodeProperty() { return classCode; }
}
