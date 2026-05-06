package com.flightcontrol.domain;

import com.flightcontrol.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightStateMachineTest {

    // ---------------------------------------------------------------------------
    // Allowed transition pairs — used to build both allowed and disallowed streams
    // ---------------------------------------------------------------------------

    private static final List<FlightStatus[]> ALLOWED_PAIRS = List.of(
            new FlightStatus[]{FlightStatus.SCHEDULED, FlightStatus.DELAYED},
            new FlightStatus[]{FlightStatus.SCHEDULED, FlightStatus.DEPARTED},
            new FlightStatus[]{FlightStatus.SCHEDULED, FlightStatus.CANCELLED},
            new FlightStatus[]{FlightStatus.DELAYED,   FlightStatus.DEPARTED},
            new FlightStatus[]{FlightStatus.DELAYED,   FlightStatus.CANCELLED},
            new FlightStatus[]{FlightStatus.DEPARTED,  FlightStatus.IN_AIR},
            new FlightStatus[]{FlightStatus.IN_AIR,    FlightStatus.LANDED}
    );

    static Stream<FlightStatus[]> allowedTransitionPairs() {
        return ALLOWED_PAIRS.stream();
    }

    static Stream<FlightStatus[]> disallowedTransitionPairs() {
        Set<String> allowedKeys = new java.util.HashSet<>();
        for (FlightStatus[] pair : ALLOWED_PAIRS) {
            allowedKeys.add(pair[0].name() + "->" + pair[1].name());
        }

        return Arrays.stream(FlightStatus.values())
                .flatMap(from -> Arrays.stream(FlightStatus.values())
                        .filter(to -> !allowedKeys.contains(from.name() + "->" + to.name()))
                        .map(to -> new FlightStatus[]{from, to}));
    }

    // ---------------------------------------------------------------------------
    // validate — allowed transitions
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1} should be allowed")
    @MethodSource("allowedTransitionPairs")
    void shouldAllowTransition_whenTransitionIsValid(FlightStatus from, FlightStatus to) {
        assertDoesNotThrow(() -> FlightStateMachine.validate(from, to));
    }

    // ---------------------------------------------------------------------------
    // validate — disallowed transitions
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1} should throw InvalidStatusTransitionException")
    @MethodSource("disallowedTransitionPairs")
    void shouldThrowException_whenTransitionIsInvalid(FlightStatus from, FlightStatus to) {
        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> FlightStateMachine.validate(from, to)
        );

        assertEquals(from, exception.getFrom());
        assertEquals(to, exception.getTo());
        assertTrue(exception.getMessage().contains(from.name()));
        assertTrue(exception.getMessage().contains(to.name()));
    }

    // ---------------------------------------------------------------------------
    // allowedTransitions — exact set per status
    // ---------------------------------------------------------------------------

    @Test
    void shouldReturnCorrectAllowedSet_forScheduled() {
        Set<FlightStatus> expected = EnumSet.of(FlightStatus.DELAYED, FlightStatus.DEPARTED, FlightStatus.CANCELLED);
        assertEquals(expected, FlightStateMachine.allowedTransitions(FlightStatus.SCHEDULED));
    }

    @Test
    void shouldReturnCorrectAllowedSet_forDelayed() {
        Set<FlightStatus> expected = EnumSet.of(FlightStatus.DEPARTED, FlightStatus.CANCELLED);
        assertEquals(expected, FlightStateMachine.allowedTransitions(FlightStatus.DELAYED));
    }

    @Test
    void shouldReturnCorrectAllowedSet_forDeparted() {
        Set<FlightStatus> expected = EnumSet.of(FlightStatus.IN_AIR);
        assertEquals(expected, FlightStateMachine.allowedTransitions(FlightStatus.DEPARTED));
    }

    @Test
    void shouldReturnCorrectAllowedSet_forInAir() {
        Set<FlightStatus> expected = EnumSet.of(FlightStatus.LANDED);
        assertEquals(expected, FlightStateMachine.allowedTransitions(FlightStatus.IN_AIR));
    }

    @Test
    void shouldReturnEmptySet_forLanded() {
        Set<FlightStatus> result = FlightStateMachine.allowedTransitions(FlightStatus.LANDED);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptySet_forCancelled() {
        Set<FlightStatus> result = FlightStateMachine.allowedTransitions(FlightStatus.CANCELLED);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------------------
    // allowedTransitions — returned set must be unmodifiable
    // ---------------------------------------------------------------------------

    @Test
    void shouldReturnUnmodifiableSet_forAllowedTransitions() {
        Set<FlightStatus> result = FlightStateMachine.allowedTransitions(FlightStatus.SCHEDULED);
        assertThrows(UnsupportedOperationException.class, () -> result.add(FlightStatus.IN_AIR));
    }

    // ---------------------------------------------------------------------------
    // Exhaustiveness guard — ensures 36 combinations are covered
    // ---------------------------------------------------------------------------

    @Test
    void shouldCoverAllThirtySixCombinations() {
        long allowed = ALLOWED_PAIRS.size();  // 7
        long total = (long) FlightStatus.values().length * FlightStatus.values().length;  // 36
        long disallowed = disallowedTransitionPairs().count();

        assertEquals(36L, total);
        assertEquals(7L, allowed);
        assertEquals(29L, disallowed);
        assertEquals(total, allowed + disallowed);
    }
}
