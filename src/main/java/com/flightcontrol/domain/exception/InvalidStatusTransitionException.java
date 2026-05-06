package com.flightcontrol.domain.exception;

import com.flightcontrol.domain.FlightStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final FlightStatus from;
    private final FlightStatus to;

    public InvalidStatusTransitionException(FlightStatus from, FlightStatus to) {
        super("Transition from " + from + " to " + to + " is not allowed");
        this.from = from;
        this.to = to;
    }

    public FlightStatus getFrom() {
        return from;
    }

    public FlightStatus getTo() {
        return to;
    }
}
