package com.flightcontrol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}\\d{3,4}$")
    @JsonProperty("flight_number")
    private String flightNumber;

    @NotBlank
    @Size(max = 100)
    @JsonProperty("airline")
    private String airline;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @JsonProperty("origin")
    private String origin;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @JsonProperty("destination")
    private String destination;

    @NotNull
    @JsonProperty("departure_time")
    private OffsetDateTime departureTime;

    @NotNull
    @JsonProperty("arrival_time")
    private OffsetDateTime arrivalTime;
}
