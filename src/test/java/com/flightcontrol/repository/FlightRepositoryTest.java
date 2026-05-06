package com.flightcontrol.repository;

import com.flightcontrol.domain.Flight;
import com.flightcontrol.domain.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.jpa.defer-datasource-initialization=true"
})
class FlightRepositoryTest {

    @Autowired
    private FlightRepository flightRepository;

    private Flight savedFlight;

    @BeforeEach
    void setUp() {
        savedFlight = flightRepository.save(buildFlight("AA123"));
    }

    // ── Save & Find ────────────────────────────────────────────────────────────

    @Test
    void shouldPreserveAllFields_whenFlightIsSaved() {
        Optional<Flight> found = flightRepository.findById(savedFlight.getId());

        assertThat(found).isPresent();
        Flight flight = found.get();
        assertThat(flight.getFlightNumber()).isEqualTo("AA123");
        assertThat(flight.getAirline()).isEqualTo("Test Airlines");
        assertThat(flight.getOrigin()).isEqualTo("JFK");
        assertThat(flight.getDestination()).isEqualTo("LAX");
        assertThat(flight.getStatus()).isEqualTo(FlightStatus.SCHEDULED);
        assertThat(flight.getDepartureTime()).isNotNull();
        assertThat(flight.getArrivalTime()).isNotNull();
        assertThat(flight.getCreatedAt()).isNotNull();
        assertThat(flight.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldAssignId_whenFlightIsSavedWithoutExplicitId() {
        // buildFlight does not set id, so @PrePersist generates it
        Flight flight = buildFlight("BB456");

        Flight result = flightRepository.save(flight);

        assertThat(result.getId()).isNotNull();
    }

    @Test
    void shouldDefaultStatusToScheduled_whenStatusIsNullOnPersist() {
        Flight flight = buildFlight("CC789");
        flight.setStatus(null);

        Flight result = flightRepository.save(flight);

        assertThat(result.getStatus()).isEqualTo(FlightStatus.SCHEDULED);
    }

    // ── existsByFlightNumber ───────────────────────────────────────────────────

    @Test
    void shouldReturnTrue_whenFlightNumberExists() {
        assertThat(flightRepository.existsByFlightNumber("AA123")).isTrue();
    }

    @Test
    void shouldReturnFalse_whenFlightNumberDoesNotExist() {
        assertThat(flightRepository.existsByFlightNumber("ZZ999")).isFalse();
    }

    // ── existsByFlightNumberAndIdNot ───────────────────────────────────────────

    @Test
    void shouldReturnFalse_whenCheckingUniquenessForSameId() {
        boolean result = flightRepository.existsByFlightNumberAndIdNot(
                savedFlight.getFlightNumber(), savedFlight.getId());

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrue_whenFlightNumberExistsUnderDifferentId() {
        UUID differentId = UUID.randomUUID();

        boolean result = flightRepository.existsByFlightNumberAndIdNot(
                savedFlight.getFlightNumber(), differentId);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenFlightNumberDoesNotExistForAnyId() {
        boolean result = flightRepository.existsByFlightNumberAndIdNot(
                "ZZ999", UUID.randomUUID());

        assertThat(result).isFalse();
    }

    // ── findAll with Pageable ──────────────────────────────────────────────────

    @Test
    void shouldReturnPaginatedResults_whenMultipleFlightsExist() {
        flightRepository.save(buildFlight("BB456"));
        flightRepository.save(buildFlight("CC789"));

        Page<Flight> page = flightRepository.findAll(PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldReturnSecondPage_whenRequestingPageOne() {
        flightRepository.save(buildFlight("BB456"));
        flightRepository.save(buildFlight("CC789"));

        Page<Flight> page = flightRepository.findAll(PageRequest.of(1, 2));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void shouldReturnEmptyPage_whenNoFlightsExist() {
        flightRepository.deleteAll();

        Page<Flight> page = flightRepository.findAll(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    // ── findAll with Specification ────────────────────────────────────────────

    @Test
    void shouldReturnFlightsMatchingSpec_whenFilteringByStatus() {
        Flight delayedFlight = buildFlight("DD321");
        delayedFlight.setStatus(FlightStatus.DELAYED);
        flightRepository.save(delayedFlight);

        Specification<Flight> scheduledOnly =
                (root, query, cb) -> cb.equal(root.get("status"), FlightStatus.SCHEDULED);

        Page<Flight> result = flightRepository.findAll(scheduledOnly, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(FlightStatus.SCHEDULED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Flight buildFlight(String flightNumber) {
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime arrival = departure.plusHours(5);
        return Flight.builder()
                .flightNumber(flightNumber)
                .airline("Test Airlines")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.SCHEDULED)
                .build();
    }
}
