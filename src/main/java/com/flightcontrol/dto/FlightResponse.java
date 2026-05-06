package com.flightcontrol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightcontrol.domain.FlightStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("flight_number")
    private String flightNumber;

    @JsonProperty("airline")
    private String airline;

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("departure_time")
    private OffsetDateTime departureTime;

    @JsonProperty("arrival_time")
    private OffsetDateTime arrivalTime;

    @JsonProperty("status")
    private FlightStatus status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
