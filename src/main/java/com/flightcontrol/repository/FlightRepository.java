package com.flightcontrol.repository;

import com.flightcontrol.domain.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FlightRepository extends JpaRepository<Flight, UUID>, JpaSpecificationExecutor<Flight> {

    boolean existsByFlightNumber(String flightNumber);

    boolean existsByFlightNumberAndIdNot(String flightNumber, UUID id);
}
