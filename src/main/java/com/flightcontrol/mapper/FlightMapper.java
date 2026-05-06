package com.flightcontrol.mapper;

import com.flightcontrol.domain.Flight;
import com.flightcontrol.dto.FlightPage;
import com.flightcontrol.dto.FlightRequest;
import com.flightcontrol.dto.FlightResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FlightMapper {

    public FlightResponse toResponse(Flight flight) {
        Objects.requireNonNull(flight, "flight must not be null");
        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .status(flight.getStatus())
                .createdAt(flight.getCreatedAt())
                .updatedAt(flight.getUpdatedAt())
                .build();
    }

    public Flight toEntity(FlightRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return Flight.builder()
                .flightNumber(request.getFlightNumber())
                .airline(request.getAirline())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .build();
    }

    public FlightPage toPage(Page<Flight> page, int pageNum, int pageSize) {
        Objects.requireNonNull(page, "page must not be null");
        List<FlightResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return FlightPage.builder()
                .items(items)
                .page(pageNum)
                .size(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
