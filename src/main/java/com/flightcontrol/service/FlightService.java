package com.flightcontrol.service;

import com.flightcontrol.domain.Flight;
import com.flightcontrol.domain.FlightStateMachine;
import com.flightcontrol.domain.FlightStatus;
import com.flightcontrol.domain.exception.BusinessRuleViolationException;
import com.flightcontrol.domain.exception.DuplicateFlightNumberException;
import com.flightcontrol.domain.exception.FlightNotDeletableException;
import com.flightcontrol.domain.exception.FlightNotFoundException;
import com.flightcontrol.domain.exception.FlightNotModifiableException;
import com.flightcontrol.dto.FlightPage;
import com.flightcontrol.dto.FlightRequest;
import com.flightcontrol.dto.FlightResponse;
import com.flightcontrol.mapper.FlightMapper;
import com.flightcontrol.repository.FlightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional
public class FlightService {

    private final FlightRepository repository;
    private final FlightMapper mapper;

    public FlightService(FlightRepository repository, FlightMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public FlightPage listFlights(String origin, String destination, FlightStatus status, int page, int size) {
        Specification<Flight> spec = buildSpecification(origin, destination, status);
        Page<Flight> result = repository.findAll(spec, PageRequest.of(page, size));
        return mapper.toPage(result, page, size);
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlight(UUID id) {
        Flight flight = findByIdOrThrow(id);
        return mapper.toResponse(flight);
    }

    public FlightResponse createFlight(FlightRequest request) {
        enforceBusinessRules(request);

        if (repository.existsByFlightNumber(request.getFlightNumber())) {
            throw new DuplicateFlightNumberException(request.getFlightNumber());
        }

        Flight entity = mapper.toEntity(request);
        Flight saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public FlightResponse updateFlight(UUID id, FlightRequest request) {
        Flight existing = findByIdOrThrow(id);

        if (existing.getStatus() != FlightStatus.SCHEDULED && existing.getStatus() != FlightStatus.DELAYED) {
            throw new FlightNotModifiableException(id, existing.getStatus());
        }

        enforceBusinessRules(request);

        if (repository.existsByFlightNumberAndIdNot(request.getFlightNumber(), id)) {
            throw new DuplicateFlightNumberException(request.getFlightNumber());
        }

        existing.setFlightNumber(request.getFlightNumber());
        existing.setAirline(request.getAirline());
        existing.setOrigin(request.getOrigin());
        existing.setDestination(request.getDestination());
        existing.setDepartureTime(request.getDepartureTime());
        existing.setArrivalTime(request.getArrivalTime());

        Flight saved = repository.save(existing);
        return mapper.toResponse(saved);
    }

    public void deleteFlight(UUID id) {
        Flight existing = findByIdOrThrow(id);

        if (existing.getStatus() != FlightStatus.SCHEDULED) {
            throw new FlightNotDeletableException(id, existing.getStatus());
        }

        repository.delete(existing);
    }

    public FlightResponse transitionStatus(UUID id, FlightStatus target) {
        Flight existing = findByIdOrThrow(id);
        FlightStateMachine.validate(existing.getStatus(), target);
        existing.setStatus(target);
        Flight saved = repository.save(existing);
        return mapper.toResponse(saved);
    }

    // --- private helpers ---

    private Flight findByIdOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));
    }

    private void enforceBusinessRules(FlightRequest request) {
        if (request.getDepartureTime() == null || request.getArrivalTime() == null) {
            throw new BusinessRuleViolationException("departure_time and arrival_time must not be null");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (!request.getDepartureTime().isAfter(now)) {
            throw new BusinessRuleViolationException(
                    "departure_time must be in the future, got: " + request.getDepartureTime());
        }

        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessRuleViolationException(
                    "arrival_time must be after departure_time");
        }

        if (request.getOrigin().equalsIgnoreCase(request.getDestination())) {
            throw new BusinessRuleViolationException(
                    "origin and destination must be different, both are: " + request.getOrigin());
        }
    }

    private Specification<Flight> buildSpecification(String origin, String destination, FlightStatus status) {
        Specification<Flight> spec = Specification.where(null);

        if (origin != null && !origin.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("origin"), origin));
        }

        if (destination != null && !destination.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("destination"), destination));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return spec;
    }
}
