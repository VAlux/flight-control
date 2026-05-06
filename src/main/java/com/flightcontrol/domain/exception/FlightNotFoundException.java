package com.flightcontrol.domain.exception;

import java.util.UUID;

public class FlightNotFoundException extends RuntimeException {

    private final UUID id;

    public FlightNotFoundException(UUID id) {
        super("Flight not found with id: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
