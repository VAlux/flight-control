package com.flightcontrol.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.flightcontrol.dto.StatusTransitionRequest;
import com.flightcontrol.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlightController.class)
@Import({RequestIdFilter.class, GlobalExceptionHandler.class, MethodValidationPostProcessor.class})
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlightService flightService;

    private UUID flightId;
    private FlightResponse sampleResponse;
    private FlightRequest validRequest;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime arrival = departure.plusHours(3);

        sampleResponse = FlightResponse.builder()
                .id(flightId)
                .flightNumber("AA123")
                .airline("American Airlines")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(FlightStatus.SCHEDULED)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        validRequest = FlightRequest.builder()
                .flightNumber("AA123")
                .airline("American Airlines")
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();
    }

    // --- GET /api/v1/flights ---

    @Test
    void shouldReturn200WithFlightPage_whenListFlightsCalled() throws Exception {
        FlightPage page = FlightPage.builder()
                .items(List.of(sampleResponse))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(flightService.listFlights(null, null, null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].flight_number", is("AA123")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.total_elements", is(1)))
                .andExpect(jsonPath("$.total_pages", is(1)));
    }

    @Test
    void shouldIncludeRequestIdHeader_whenListFlightsCalled() throws Exception {
        FlightPage page = FlightPage.builder()
                .items(List.of())
                .page(0).size(20).totalElements(0L).totalPages(0)
                .build();
        when(flightService.listFlights(null, null, null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldEchoProvidedRequestId_whenXRequestIdHeaderSupplied() throws Exception {
        FlightPage page = FlightPage.builder()
                .items(List.of())
                .page(0).size(20).totalElements(0L).totalPages(0)
                .build();
        when(flightService.listFlights(null, null, null, 0, 20)).thenReturn(page);

        String providedId = "my-correlation-id-123";
        mockMvc.perform(get("/api/v1/flights")
                        .header("X-Request-ID", providedId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", providedId));
    }

    // --- POST /api/v1/flights ---

    @Test
    void shouldReturn201WithLocationHeader_whenCreateFlightCalledWithValidBody() throws Exception {
        when(flightService.createFlight(any(FlightRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/flights/" + flightId)))
                .andExpect(jsonPath("$.id", is(flightId.toString())))
                .andExpect(jsonPath("$.flight_number", is("AA123")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn400WithErrorsArray_whenCreateFlightCalledWithMissingAirline() throws Exception {
        FlightRequest requestWithoutAirline = FlightRequest.builder()
                .flightNumber("AA123")
                // airline intentionally omitted
                .origin("JFK")
                .destination("LAX")
                .departureTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                .arrivalTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(3))
                .build();

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutAirline)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("validation-failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.errors[0].field", is("airline")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn400WithProblemDetail_whenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("validation-failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- GET /api/v1/flights/{flightId} ---

    @Test
    void shouldReturn200WithFlight_whenGetFlightCalledWithExistingId() throws Exception {
        when(flightService.getFlight(flightId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/flights/{id}", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(flightId.toString())))
                .andExpect(jsonPath("$.flight_number", is("AA123")))
                .andExpect(jsonPath("$.airline", is("American Airlines")))
                .andExpect(jsonPath("$.origin", is("JFK")))
                .andExpect(jsonPath("$.destination", is("LAX")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn404WithFlightNotFoundType_whenGetFlightCalledWithUnknownId() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(flightService.getFlight(unknownId)).thenThrow(new FlightNotFoundException(unknownId));

        mockMvc.perform(get("/api/v1/flights/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("flight-not-found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Flight Not Found")))
                .andExpect(jsonPath("$.detail", notNullValue()))
                .andExpect(jsonPath("$.instance", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.correlation_id", notNullValue()))
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- PUT /api/v1/flights/{flightId} ---

    @Test
    void shouldReturn200WithUpdatedFlight_whenUpdateFlightCalledWithValidBody() throws Exception {
        when(flightService.updateFlight(eq(flightId), any(FlightRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/flights/{id}", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(flightId.toString())))
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- DELETE /api/v1/flights/{flightId} ---

    @Test
    void shouldReturn204_whenDeleteFlightCalledWithExistingId() throws Exception {
        doNothing().when(flightService).deleteFlight(flightId);

        mockMvc.perform(delete("/api/v1/flights/{id}", flightId))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- PATCH /api/v1/flights/{flightId}/status ---

    @Test
    void shouldReturn200WithUpdatedFlight_whenTransitionStatusCalledWithValidTransition() throws Exception {
        FlightResponse delayedResponse = FlightResponse.builder()
                .id(flightId)
                .flightNumber("AA123")
                .airline("American Airlines")
                .origin("JFK")
                .destination("LAX")
                .departureTime(sampleResponse.getDepartureTime())
                .arrivalTime(sampleResponse.getArrivalTime())
                .status(FlightStatus.DELAYED)
                .createdAt(sampleResponse.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(flightService.transitionStatus(flightId, FlightStatus.DELAYED)).thenReturn(delayedResponse);

        StatusTransitionRequest transitionRequest = StatusTransitionRequest.builder()
                .status(FlightStatus.DELAYED)
                .build();

        mockMvc.perform(patch("/api/v1/flights/{id}/status", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELAYED")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn409WithInvalidStatusTransitionType_whenInvalidTransitionRequested() throws Exception {
        doThrow(new InvalidStatusTransitionException(FlightStatus.LANDED, FlightStatus.SCHEDULED))
                .when(flightService).transitionStatus(flightId, FlightStatus.SCHEDULED);

        StatusTransitionRequest transitionRequest = StatusTransitionRequest.builder()
                .status(FlightStatus.SCHEDULED)
                .build();

        mockMvc.perform(patch("/api/v1/flights/{id}/status", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("invalid-status-transition")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.title", is("Invalid Status Transition")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()))
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- Exception handler coverage (HIGH 2) ---

    @Test
    void shouldReturn422WithErrorsArray_whenCreateFlightViolatesBusinessRule() throws Exception {
        when(flightService.createFlight(any()))
                .thenThrow(new BusinessRuleViolationException("origin and destination must not be equal"));

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("validation-failed")))
                .andExpect(jsonPath("$.errors[0].field", notNullValue()))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn409WithDuplicateType_whenCreateFlightHasDuplicateNumber() throws Exception {
        when(flightService.createFlight(any()))
                .thenThrow(new DuplicateFlightNumberException("AA123"));

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("duplicate-flight-number")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn409WithNotModifiableType_whenUpdateFlightIsNotModifiable() throws Exception {
        when(flightService.updateFlight(any(), any()))
                .thenThrow(new FlightNotModifiableException(flightId, FlightStatus.DEPARTED));

        mockMvc.perform(put("/api/v1/flights/{id}", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("flight-not-modifiable")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn404_whenDeleteFlightNotFound() throws Exception {
        doThrow(new FlightNotFoundException(flightId))
                .when(flightService).deleteFlight(any());

        mockMvc.perform(delete("/api/v1/flights/{id}", flightId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("flight-not-found")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn409WithNotDeletableType_whenDeleteFlightIsNotDeletable() throws Exception {
        doThrow(new FlightNotDeletableException(flightId, FlightStatus.DEPARTED))
                .when(flightService).deleteFlight(any());

        mockMvc.perform(delete("/api/v1/flights/{id}", flightId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("flight-not-deletable")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void shouldReturn400_whenTransitionStatusBodyIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/flights/{id}/status", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- PUT error path (MEDIUM 6) ---

    @Test
    void shouldReturn400_whenUpdateFlightBodyIsInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/flights/{id}", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(header().exists("X-Request-ID"));
    }

    // --- ConstraintViolationException handler coverage ---

    @Test
    void shouldReturn400_whenSizeExceedsMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/flights").param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("validation-failed")));
    }

    // --- MethodArgumentTypeMismatchException handler coverage ---

    @Test
    void shouldReturn400_whenStatusQueryParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/flights").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("validation-failed")));
    }

    // --- Catch-all Exception handler coverage ---

    @Test
    void shouldReturn500WithProblemJson_whenUnexpectedExceptionOccurs() throws Exception {
        when(flightService.listFlights(any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", containsString("internal-error")));
    }
}
