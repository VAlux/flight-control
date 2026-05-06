package com.flightcontrol.mapper;

import com.flightcontrol.domain.Flight;
import com.flightcontrol.domain.FlightStatus;
import com.flightcontrol.dto.FlightPage;
import com.flightcontrol.dto.FlightRequest;
import com.flightcontrol.dto.FlightResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlightMapperTest {

    private FlightMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FlightMapper();
    }

    @Test
    void shouldMapAllFields_whenConvertingFlightToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        OffsetDateTime departure = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime arrival = OffsetDateTime.of(2026, 6, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 5, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime updatedAt = OffsetDateTime.of(2026, 5, 2, 9, 0, 0, 0, ZoneOffset.UTC);

        Flight flight = Flight.builder()
                .id(id)
                .flightNumber("AB1234")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.SCHEDULED)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // Act
        FlightResponse response = mapper.toResponse(flight);

        // Assert
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getFlightNumber()).isEqualTo("AB1234");
        assertThat(response.getAirline()).isEqualTo("Test Airline");
        assertThat(response.getOrigin()).isEqualTo("JFK");
        assertThat(response.getDestination()).isEqualTo("LAX");
        assertThat(response.getDepartureTime()).isEqualTo(departure);
        assertThat(response.getArrivalTime()).isEqualTo(arrival);
        assertThat(response.getStatus()).isEqualTo(FlightStatus.SCHEDULED);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldMapAllFields_whenConvertingFlightRequestToEntity() {
        // Arrange
        OffsetDateTime departure = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime arrival = OffsetDateTime.of(2026, 6, 1, 14, 0, 0, 0, ZoneOffset.UTC);

        FlightRequest request = FlightRequest.builder()
                .flightNumber("AB1234")
                .airline("Test Airline")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        // Act
        Flight entity = mapper.toEntity(request);

        // Assert
        assertThat(entity.getFlightNumber()).isEqualTo("AB1234");
        assertThat(entity.getAirline()).isEqualTo("Test Airline");
        assertThat(entity.getOrigin()).isEqualTo("JFK");
        assertThat(entity.getDestination()).isEqualTo("LAX");
        assertThat(entity.getDepartureTime()).isEqualTo(departure);
        assertThat(entity.getArrivalTime()).isEqualTo(arrival);
        // id, status, and timestamps are left null for @PrePersist to populate
        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void shouldMapPageCorrectly_whenConvertingSpringPage() {
        // Arrange
        OffsetDateTime departure = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime arrival = OffsetDateTime.of(2026, 6, 1, 14, 0, 0, 0, ZoneOffset.UTC);

        Flight flight1 = Flight.builder()
                .id(UUID.randomUUID())
                .flightNumber("AB1234")
                .airline("Airline One")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.SCHEDULED)
                .createdAt(departure)
                .updatedAt(departure)
                .build();

        Flight flight2 = Flight.builder()
                .id(UUID.randomUUID())
                .flightNumber("CD5678")
                .airline("Airline Two")
                .origin("ORD")
                .destination("MIA")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.DELAYED)
                .createdAt(departure)
                .updatedAt(departure)
                .build();

        List<Flight> flights = List.of(flight1, flight2);
        Page<Flight> springPage = new PageImpl<>(flights, PageRequest.of(0, 10), 25L);

        // Act
        FlightPage result = mapper.toPage(springPage, 0, 10);

        // Assert
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(25L);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getItems().get(0).getFlightNumber()).isEqualTo("AB1234");
        assertThat(result.getItems().get(1).getFlightNumber()).isEqualTo("CD5678");
    }

    @Test
    void shouldRejectInvalidFlightNumber_whenValidatingRequest() {
        // Arrange
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            FlightRequest request = FlightRequest.builder()
                    .flightNumber("invalid")
                    .airline("Test Airline")
                    .origin("JFK")
                    .destination("LAX")
                    .departureTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                    .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(4))
                    .build();

            // Act
            Set<ConstraintViolation<FlightRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("flightNumber"));
        }
    }

    @Test
    void shouldPassValidation_whenAllFieldsAreValid() {
        // Arrange
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            FlightRequest request = FlightRequest.builder()
                    .flightNumber("AA123")
                    .airline("Test Air")
                    .origin("JFK")
                    .destination("LAX")
                    .departureTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                    .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2))
                    .build();

            // Act
            Set<ConstraintViolation<FlightRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void shouldRejectBlankAirline_whenAirlineIsEmpty() {
        // Arrange
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            FlightRequest request = FlightRequest.builder()
                    .flightNumber("AA123")
                    .airline("")
                    .origin("JFK")
                    .destination("LAX")
                    .departureTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                    .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2))
                    .build();

            // Act
            Set<ConstraintViolation<FlightRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("airline"));
        }
    }

    @Test
    void shouldRejectInvalidOrigin_whenOriginIsLowercase() {
        // Arrange
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            FlightRequest request = FlightRequest.builder()
                    .flightNumber("AA123")
                    .airline("Test Air")
                    .origin("jfk")
                    .destination("LAX")
                    .departureTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                    .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2))
                    .build();

            // Act
            Set<ConstraintViolation<FlightRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("origin"));
        }
    }

    @Test
    void shouldThrowNullPointerException_whenFlightIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> mapper.toResponse(null));
    }

    @Test
    void shouldThrowNullPointerException_whenRequestIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> mapper.toEntity(null));
    }
}
