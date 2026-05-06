package com.flightcontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "flight")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private UUID id;

    @Setter
    @Column(name = "flight_number", length = 6, nullable = false, unique = true)
    private String flightNumber;

    @Setter
    @Column(name = "airline", length = 100, nullable = false)
    private String airline;

    @Setter
    @Column(name = "origin", length = 3, nullable = false)
    private String origin;

    @Setter
    @Column(name = "destination", length = 3, nullable = false)
    private String destination;

    @Setter
    @Column(name = "departure_time", nullable = false, columnDefinition = "TIMESTAMP")
    private OffsetDateTime departureTime;

    @Setter
    @Column(name = "arrival_time", nullable = false, columnDefinition = "TIMESTAMP")
    private OffsetDateTime arrivalTime;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FlightStatus status;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = FlightStatus.SCHEDULED;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
