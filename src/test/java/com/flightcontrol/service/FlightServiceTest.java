package com.flightcontrol.service;

import com.flightcontrol.domain.Flight;
import com.flightcontrol.domain.FlightStatus;
import com.flightcontrol.domain.exception.BusinessRuleViolationException;
import com.flightcontrol.domain.exception.DuplicateFlightNumberException;
import com.flightcontrol.domain.exception.FlightNotDeletableException;
import com.flightcontrol.domain.exception.FlightNotFoundException;
import com.flightcontrol.domain.exception.FlightNotModifiableException;
import com.flightcontrol.domain.exception.InvalidStatusTransitionException;
import com.flightcontrol.dto.FlightPage;
import com.flightcontrol.dto.FlightRequest;
import com.flightcontrol.dto.FlightResponse;
import com.flightcontrol.mapper.FlightMapper;
import com.flightcontrol.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository repository;

    @Mock
    private FlightMapper mapper;

    private FlightService service;

    // Shared test data
    private UUID flightId;
    private Flight scheduledFlight;
    private FlightRequest validRequest;
    private FlightResponse expectedResponse;

    @BeforeEach
    void setUp() {
        service = new FlightService(repository, mapper);

        flightId = UUID.randomUUID();

        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime arrival = departure.plusHours(3);

        scheduledFlight = Flight.builder()
                .id(flightId)
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.SCHEDULED)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        validRequest = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        expectedResponse = FlightResponse.builder()
                .id(flightId)
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.SCHEDULED)
                .build();
    }

    // --- listFlights ---

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnFlightPage_whenListFlightsCalledWithNoFilters() {
        // Arrange
        Page<Flight> page = new PageImpl<>(List.of(scheduledFlight), PageRequest.of(0, 10), 1);
        FlightPage expectedPage = FlightPage.builder()
                .items(List.of(expectedResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(mapper.toPage(page, 0, 10)).thenReturn(expectedPage);

        // Act
        FlightPage result = service.listFlights(null, null, null, 0, 10);

        // Assert
        assertThat(result).isEqualTo(expectedPage);
        verify(repository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldApplyAllFilters_whenListFlightsCalledWithOriginDestinationAndStatus() {
        // Arrange
        Page<Flight> page = new PageImpl<>(List.of(scheduledFlight), PageRequest.of(0, 5), 1);
        FlightPage expectedPage = FlightPage.builder()
                .items(List.of(expectedResponse))
                .page(0)
                .size(5)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(mapper.toPage(page, 0, 5)).thenReturn(expectedPage);

        // Act
        FlightPage result = service.listFlights("JFK", "LAX", FlightStatus.SCHEDULED, 0, 5);

        // Assert
        assertThat(result).isEqualTo(expectedPage);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotAddOriginPredicate_whenOriginIsBlank() {
        // Arrange — blank origin should be treated identically to null (no filter applied)
        Page<Flight> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        FlightPage expectedPage = FlightPage.builder()
                .items(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);
        when(mapper.toPage(emptyPage, 0, 10)).thenReturn(expectedPage);

        // Act
        FlightPage result = service.listFlights(" ", null, null, 0, 10);

        // Assert — repository is still called (no crash, no NPE, no origin predicate)
        assertThat(result).isEqualTo(expectedPage);
        verify(repository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }

    // --- getFlight ---

    @Test
    void shouldReturnFlightResponse_whenFlightExists() {
        // Arrange
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));
        when(mapper.toResponse(scheduledFlight)).thenReturn(expectedResponse);

        // Act
        FlightResponse result = service.getFlight(flightId);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void shouldThrowFlightNotFoundException_whenFlightDoesNotExist() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getFlight(unknownId))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // --- createFlight ---

    @Test
    void shouldCreateAndReturnFlight_whenRequestIsValid() {
        // Arrange
        when(repository.existsByFlightNumber("AB123")).thenReturn(false);
        when(mapper.toEntity(validRequest)).thenReturn(scheduledFlight);
        when(repository.save(scheduledFlight)).thenReturn(scheduledFlight);
        when(mapper.toResponse(scheduledFlight)).thenReturn(expectedResponse);

        // Act
        FlightResponse result = service.createFlight(validRequest);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(repository).save(scheduledFlight);
    }

    @Test
    void shouldThrowDuplicateFlightNumberException_whenFlightNumberAlreadyExists() {
        // Arrange
        when(repository.existsByFlightNumber("AB123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.createFlight(validRequest))
                .isInstanceOf(DuplicateFlightNumberException.class)
                .hasMessageContaining("AB123");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleViolationException_whenDepartureTimeIsInThePast() {
        // Arrange
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusHours(2))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.createFlight(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("departure_time");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleViolationException_whenArrivalTimeIsBeforeDepartureTime() {
        // Arrange
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(departure.minusHours(1))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.createFlight(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("arrival_time");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleViolationException_whenArrivalTimeEqualsDepatureTime() {
        // Arrange
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(departure)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.createFlight(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("arrival_time");
    }

    @Test
    void shouldThrowBusinessRuleViolationException_whenOriginEqualsDestination() {
        // Arrange
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("JFK")
                .departureTime(departure)
                .arrivalTime(departure.plusHours(3))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.createFlight(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("origin");
    }

    // --- updateFlight ---

    @Test
    void shouldUpdateAndReturnFlight_whenFlightIsScheduledAndRequestIsValid() {
        // Arrange
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));
        when(repository.existsByFlightNumberAndIdNot("AB123", flightId)).thenReturn(false);
        when(repository.save(scheduledFlight)).thenReturn(scheduledFlight);
        when(mapper.toResponse(scheduledFlight)).thenReturn(expectedResponse);

        // Act
        FlightResponse result = service.updateFlight(flightId, validRequest);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(repository).save(scheduledFlight);
    }

    @Test
    void shouldUpdateAndReturnFlight_whenFlightIsDelayedAndRequestIsValid() {
        // Arrange
        Flight delayedFlight = buildFlight(flightId, FlightStatus.DELAYED);
        when(repository.findById(flightId)).thenReturn(Optional.of(delayedFlight));
        when(repository.existsByFlightNumberAndIdNot("AB123", flightId)).thenReturn(false);
        when(repository.save(delayedFlight)).thenReturn(delayedFlight);
        when(mapper.toResponse(delayedFlight)).thenReturn(expectedResponse);

        // Act
        FlightResponse result = service.updateFlight(flightId, validRequest);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void shouldThrowFlightNotModifiableException_whenFlightIsDeparted() {
        // Arrange
        Flight departedFlight = buildFlight(flightId, FlightStatus.DEPARTED);
        when(repository.findById(flightId)).thenReturn(Optional.of(departedFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, validRequest))
                .isInstanceOf(FlightNotModifiableException.class)
                .hasMessageContaining(flightId.toString())
                .hasMessageContaining("DEPARTED");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowFlightNotModifiableException_whenFlightIsInAir() {
        // Arrange
        Flight inAirFlight = buildFlight(flightId, FlightStatus.IN_AIR);
        when(repository.findById(flightId)).thenReturn(Optional.of(inAirFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, validRequest))
                .isInstanceOf(FlightNotModifiableException.class)
                .hasMessageContaining("IN_AIR");
    }

    @Test
    void shouldThrowFlightNotModifiableException_whenFlightIsLanded() {
        // Arrange
        Flight landedFlight = buildFlight(flightId, FlightStatus.LANDED);
        when(repository.findById(flightId)).thenReturn(Optional.of(landedFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, validRequest))
                .isInstanceOf(FlightNotModifiableException.class)
                .hasMessageContaining("LANDED");
    }

    @Test
    void shouldThrowFlightNotModifiableException_whenFlightIsCancelled() {
        // Arrange
        Flight cancelledFlight = buildFlight(flightId, FlightStatus.CANCELLED);
        when(repository.findById(flightId)).thenReturn(Optional.of(cancelledFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, validRequest))
                .isInstanceOf(FlightNotModifiableException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void shouldThrowDuplicateFlightNumberException_whenFlightNumberTakenByOtherFlight() {
        // Arrange
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));
        when(repository.existsByFlightNumberAndIdNot("AB123", flightId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, validRequest))
                .isInstanceOf(DuplicateFlightNumberException.class)
                .hasMessageContaining("AB123");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowFlightNotFoundException_whenUpdatingNonExistentFlight() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(unknownId, validRequest))
                .isInstanceOf(FlightNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleViolationException_whenUpdateHasDepartureInPast() {
        // Arrange
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusHours(2))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("departure_time");
    }

    @Test
    void shouldThrowBusinessRuleViolationException_whenUpdateHasOriginEqualsDestination() {
        // Arrange
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("LAX")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(departure.plusHours(3))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("origin");
    }

    @Test
    void shouldThrowBusinessRuleViolation_whenUpdateFlightArrivalTimeNotAfterDeparture() {
        // Arrange
        Flight scheduled = buildFlight(flightId, FlightStatus.SCHEDULED);
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduled));
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(departure)   // equal to departure — not after
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.updateFlight(flightId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("arrival_time");

        verify(repository, never()).save(any());
    }

    // --- deleteFlight ---

    @Test
    void shouldDeleteFlight_whenFlightIsScheduled() {
        // Arrange
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));

        // Act
        service.deleteFlight(flightId);

        // Assert
        verify(repository).delete(scheduledFlight);
    }

    @Test
    void shouldThrowFlightNotDeletableException_whenFlightIsDelayed() {
        // Arrange
        Flight delayedFlight = buildFlight(flightId, FlightStatus.DELAYED);
        when(repository.findById(flightId)).thenReturn(Optional.of(delayedFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteFlight(flightId))
                .isInstanceOf(FlightNotDeletableException.class)
                .hasMessageContaining(flightId.toString())
                .hasMessageContaining("DELAYED");

        verify(repository, never()).delete(any(Flight.class));
    }

    @Test
    void shouldThrowFlightNotDeletableException_whenFlightIsDeparted() {
        // Arrange
        Flight departedFlight = buildFlight(flightId, FlightStatus.DEPARTED);
        when(repository.findById(flightId)).thenReturn(Optional.of(departedFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteFlight(flightId))
                .isInstanceOf(FlightNotDeletableException.class)
                .hasMessageContaining("DEPARTED");
    }

    @Test
    void shouldThrowFlightNotDeletableException_whenFlightIsInAir() {
        // Arrange
        Flight inAirFlight = buildFlight(flightId, FlightStatus.IN_AIR);
        when(repository.findById(flightId)).thenReturn(Optional.of(inAirFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteFlight(flightId))
                .isInstanceOf(FlightNotDeletableException.class)
                .hasMessageContaining("IN_AIR");
    }

    @Test
    void shouldThrowFlightNotDeletableException_whenFlightIsLanded() {
        // Arrange
        Flight landedFlight = buildFlight(flightId, FlightStatus.LANDED);
        when(repository.findById(flightId)).thenReturn(Optional.of(landedFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteFlight(flightId))
                .isInstanceOf(FlightNotDeletableException.class)
                .hasMessageContaining("LANDED");
    }

    @Test
    void shouldThrowFlightNotDeletableException_whenFlightIsCancelled() {
        // Arrange
        Flight cancelledFlight = buildFlight(flightId, FlightStatus.CANCELLED);
        when(repository.findById(flightId)).thenReturn(Optional.of(cancelledFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteFlight(flightId))
                .isInstanceOf(FlightNotDeletableException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void shouldThrowFlightNotFoundException_whenDeletingNonExistentFlight() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.deleteFlight(unknownId))
                .isInstanceOf(FlightNotFoundException.class);
    }

    // --- transitionStatus ---

    @Test
    void shouldTransitionStatus_whenTransitionIsAllowed() {
        // Arrange
        Flight savedFlight = buildFlight(flightId, FlightStatus.DELAYED);
        FlightResponse delayedResponse = FlightResponse.builder()
                .id(flightId)
                .status(FlightStatus.DELAYED)
                .build();
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));
        when(repository.save(scheduledFlight)).thenReturn(savedFlight);
        when(mapper.toResponse(savedFlight)).thenReturn(delayedResponse);

        // Act
        FlightResponse result = service.transitionStatus(flightId, FlightStatus.DELAYED);

        // Assert
        assertThat(result.getStatus()).isEqualTo(FlightStatus.DELAYED);
        verify(repository).save(scheduledFlight);
    }

    @Test
    void shouldThrowInvalidStatusTransitionException_whenTransitionIsNotAllowed() {
        // Arrange — SCHEDULED → IN_AIR is not allowed
        when(repository.findById(flightId)).thenReturn(Optional.of(scheduledFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.transitionStatus(flightId, FlightStatus.IN_AIR))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidStatusTransitionException_whenAttemptingTransitionFromTerminalStatus() {
        // Arrange — LANDED → SCHEDULED is not allowed (terminal state)
        Flight landedFlight = buildFlight(flightId, FlightStatus.LANDED);
        when(repository.findById(flightId)).thenReturn(Optional.of(landedFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.transitionStatus(flightId, FlightStatus.SCHEDULED))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidStatusTransitionException_whenCancelledFlightTransitionsToAny() {
        // Arrange — CANCELLED → SCHEDULED is not allowed (terminal state)
        Flight cancelledFlight = buildFlight(flightId, FlightStatus.CANCELLED);
        when(repository.findById(flightId)).thenReturn(Optional.of(cancelledFlight));

        // Act & Assert
        assertThatThrownBy(() -> service.transitionStatus(flightId, FlightStatus.SCHEDULED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void shouldThrowFlightNotFoundException_whenTransitioningNonExistentFlight() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.transitionStatus(unknownId, FlightStatus.DELAYED))
                .isInstanceOf(FlightNotFoundException.class);
    }

    // --- private helpers ---

    private Flight buildFlight(UUID id, FlightStatus status) {
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        return Flight.builder()
                .id(id)
                .flightNumber("AB123")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(departure.plusHours(3))
                .status(status)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
