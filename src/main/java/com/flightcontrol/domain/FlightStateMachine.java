package com.flightcontrol.domain;

import com.flightcontrol.domain.exception.InvalidStatusTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class FlightStateMachine {

    private static final Map<FlightStatus, Set<FlightStatus>> ALLOWED_TRANSITIONS;

    static {
        EnumMap<FlightStatus, Set<FlightStatus>> transitions = new EnumMap<>(FlightStatus.class);
        transitions.put(FlightStatus.SCHEDULED, EnumSet.of(FlightStatus.DELAYED, FlightStatus.DEPARTED, FlightStatus.CANCELLED));
        transitions.put(FlightStatus.DELAYED,   EnumSet.of(FlightStatus.DEPARTED, FlightStatus.CANCELLED));
        transitions.put(FlightStatus.DEPARTED,  EnumSet.of(FlightStatus.IN_AIR));
        transitions.put(FlightStatus.IN_AIR,    EnumSet.of(FlightStatus.LANDED));
        transitions.put(FlightStatus.LANDED,    EnumSet.noneOf(FlightStatus.class));
        transitions.put(FlightStatus.CANCELLED, EnumSet.noneOf(FlightStatus.class));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private FlightStateMachine() {
        // utility class — no instances
    }

    /**
     * Validates that a transition from {@code from} to {@code to} is permitted by the state machine.
     *
     * @param from current status
     * @param to   target status
     * @throws InvalidStatusTransitionException if the transition is not in the allowed set
     */
    public static void validate(FlightStatus from, FlightStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status arguments must not be null");
        }
        if (!ALLOWED_TRANSITIONS.get(from).contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    /**
     * Returns the set of statuses that are reachable from {@code from}.
     * Terminal statuses return an empty set. The returned set is unmodifiable.
     *
     * @param from current status
     * @return unmodifiable, non-null set of allowed next statuses
     */
    public static Set<FlightStatus> allowedTransitions(FlightStatus from) {
        if (from == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(from));
    }
}
