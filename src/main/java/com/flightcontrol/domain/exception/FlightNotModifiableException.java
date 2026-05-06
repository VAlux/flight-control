package com.flightcontrol.domain.exception;

import com.flightcontrol.domain.FlightStatus;

import java.util.UUID;

public class FlightNotModifiableException extends RuntimeException {

    private final UUID id;
    private final FlightStatus currentStatus;

    public FlightNotModifiableException(UUID id, FlightStatus currentStatus) {
        super("Flight " + id + " cannot be modified in status: " + currentStatus);
        this.id = id;
        this.currentStatus = currentStatus;
    }

    public UUID getId() {
        return id;
    }

    public FlightStatus getCurrentStatus() {
        return currentStatus;
    }
}
