package com.flightcontrol.dto;

import com.flightcontrol.domain.FlightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusTransitionRequest {

    @NotNull
    private FlightStatus status;
}
