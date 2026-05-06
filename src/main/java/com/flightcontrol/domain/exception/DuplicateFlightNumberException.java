package com.flightcontrol.domain.exception;

public class DuplicateFlightNumberException extends RuntimeException {

    private final String flightNumber;

    public DuplicateFlightNumberException(String flightNumber) {
        super("Flight number already exists: " + flightNumber);
        this.flightNumber = flightNumber;
    }

    public String getFlightNumber() {
        return flightNumber;
    }
}
