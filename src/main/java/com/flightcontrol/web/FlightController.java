package com.flightcontrol.web;

import com.flightcontrol.domain.FlightStatus;
import com.flightcontrol.dto.FlightPage;
import com.flightcontrol.dto.FlightRequest;
import com.flightcontrol.dto.FlightResponse;
import com.flightcontrol.dto.StatusTransitionRequest;
import com.flightcontrol.service.FlightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public ResponseEntity<FlightPage> listFlights(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) FlightStatus status,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size) {

        FlightPage flightPage = flightService.listFlights(origin, destination, status, page, size);
        return ResponseEntity.ok(flightPage);
    }

    @PostMapping
    public ResponseEntity<FlightResponse> createFlight(@RequestBody @Valid FlightRequest request) {
        FlightResponse created = flightService.createFlight(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{flightId}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable UUID flightId) {
        return ResponseEntity.ok(flightService.getFlight(flightId));
    }

    @PutMapping("/{flightId}")
    public ResponseEntity<FlightResponse> updateFlight(
            @PathVariable UUID flightId,
            @RequestBody @Valid FlightRequest request) {

        return ResponseEntity.ok(flightService.updateFlight(flightId, request));
    }

    @DeleteMapping("/{flightId}")
    public ResponseEntity<Void> deleteFlight(@PathVariable UUID flightId) {
        flightService.deleteFlight(flightId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{flightId}/status")
    public ResponseEntity<FlightResponse> transitionStatus(
            @PathVariable UUID flightId,
            @RequestBody @Valid StatusTransitionRequest request) {

        return ResponseEntity.ok(flightService.transitionStatus(flightId, request.getStatus()));
    }
}
