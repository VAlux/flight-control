package com.flightcontrol.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightcontrol.dto.FlightRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test exercising the full flight lifecycle against a real H2 database.
 * Tests are ordered and share state via static fields to simulate a realistic workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlightLifecycleIntegrationTest {

    private static final String FLIGHTS_BASE_URL = "/api/v1/flights";
    private static final String FLIGHT_NUMBER = "AA123";

    // Shared state between ordered test methods
    private static UUID createdFlightId;
    private static UUID secondFlightId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FlightRequest buildValidRequest(String flightNumber, String airline) {
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime arrival = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        return FlightRequest.builder()
                .flightNumber(flightNumber)
                .airline(airline)
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private String statusBody(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    // ── Test scenarios ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void createFlight_returns201() throws Exception {
        FlightRequest request = buildValidRequest(FLIGHT_NUMBER, "Test Air");

        MvcResult result = mockMvc.perform(post(FLIGHTS_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.flight_number", is(FLIGHT_NUMBER)))
                .andExpect(jsonPath("$.airline", is("Test Air")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertNotNull(location, "Location header must not be null");
        String idString = location.substring(location.lastIndexOf('/') + 1);
        createdFlightId = UUID.fromString(idString);
    }

    @Test
    @Order(2)
    void getFlight_returns200WithCorrectFields() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        mockMvc.perform(get(FLIGHTS_BASE_URL + "/" + createdFlightId))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.id", is(createdFlightId.toString())))
                .andExpect(jsonPath("$.flight_number", is(FLIGHT_NUMBER)))
                .andExpect(jsonPath("$.airline", is("Test Air")))
                .andExpect(jsonPath("$.origin", is("JFK")))
                .andExpect(jsonPath("$.destination", is("LAX")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.departure_time", notNullValue()))
                .andExpect(jsonPath("$.arrival_time", notNullValue()))
                .andExpect(jsonPath("$.created_at", notNullValue()))
                .andExpect(jsonPath("$.updated_at", notNullValue()));
    }

    @Test
    @Order(3)
    void listFlights_returns200WithPagination() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        mockMvc.perform(get(FLIGHTS_BASE_URL)
                        .param("origin", "JFK")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].origin", is("JFK")))
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    @Order(4)
    void transitionToDelayed_returns200() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        mockMvc.perform(patch(FLIGHTS_BASE_URL + "/" + createdFlightId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("DELAYED")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.status", is("DELAYED")));
    }

    @Test
    @Order(5)
    void updateDelayedFlight_returns200() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        FlightRequest updateRequest = buildValidRequest(FLIGHT_NUMBER, "Updated Air");

        mockMvc.perform(put(FLIGHTS_BASE_URL + "/" + createdFlightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.airline", is("Updated Air")))
                .andExpect(jsonPath("$.status", is("DELAYED")));
    }

    @Test
    @Order(6)
    void transitionToDeparted_returns200() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        mockMvc.perform(patch(FLIGHTS_BASE_URL + "/" + createdFlightId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("DEPARTED")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.status", is("DEPARTED")));
    }

    @Test
    @Order(7)
    void updateDepartedFlight_returns409() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        FlightRequest updateRequest = buildValidRequest(FLIGHT_NUMBER, "Any Airline");

        mockMvc.perform(put(FLIGHTS_BASE_URL + "/" + createdFlightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.type", containsString("flight-not-modifiable")));
    }

    @Test
    @Order(8)
    void deleteDepartedFlight_returns409() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        mockMvc.perform(delete(FLIGHTS_BASE_URL + "/" + createdFlightId))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.type", containsString("flight-not-deletable")));
    }

    @Test
    @Order(9)
    void transitionToInAirToLanded_returns200() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        // Transition DEPARTED → IN_AIR
        mockMvc.perform(patch(FLIGHTS_BASE_URL + "/" + createdFlightId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_AIR")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.status", is("IN_AIR")));

        // Transition IN_AIR → LANDED
        mockMvc.perform(patch(FLIGHTS_BASE_URL + "/" + createdFlightId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("LANDED")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.status", is("LANDED")));
    }

    @Test
    @Order(10)
    void transitionFromLanded_returns409() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        mockMvc.perform(patch(FLIGHTS_BASE_URL + "/" + createdFlightId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("CANCELLED")))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.type", containsString("invalid-status-transition")));
    }

    @Test
    @Order(11)
    void createDuplicateFlightNumber_returns409() throws Exception {
        Assumptions.assumeTrue(createdFlightId != null, "Prerequisite: flight from Order 1 must exist");
        FlightRequest duplicateRequest = buildValidRequest(FLIGHT_NUMBER, "Duplicate Airline");

        mockMvc.perform(post(FLIGHTS_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.type", containsString("duplicate-flight-number")));
    }

    @Test
    @Order(12)
    void deleteScheduledFlight_returns204ThenGet404() throws Exception {
        // Create a new SCHEDULED flight
        FlightRequest newRequest = buildValidRequest("BB456", "New Airline");

        MvcResult result = mockMvc.perform(post(FLIGHTS_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(newRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-ID"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertNotNull(location, "Location header must not be null");
        String idString = location.substring(location.lastIndexOf('/') + 1);
        secondFlightId = UUID.fromString(idString);

        // DELETE the SCHEDULED flight — should succeed with 204
        mockMvc.perform(delete(FLIGHTS_BASE_URL + "/" + secondFlightId))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-Request-ID"));

        // GET the deleted flight — should return 404
        mockMvc.perform(get(FLIGHTS_BASE_URL + "/" + secondFlightId))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.type", containsString("flight-not-found")));
    }
}
