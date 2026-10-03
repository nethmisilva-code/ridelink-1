package com.ridelink.ride.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Core Ride document owned by Ride Management Service. Passenger, driver and
 * payment are referenced only by stable identifiers owned by other services.
 * See {@link RideStatus} for the lifecycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rides")
@CompoundIndexes({
        @CompoundIndex(name = "rider_status_idx", def = "{'riderId': 1, 'status': 1}"),
        @CompoundIndex(name = "driver_status_idx", def = "{'driverId': 1, 'status': 1}")
})
public class Ride {

    @Id
    private String id;

    @Indexed
    private String riderId;          // passenger account id (Account Service)

    private String riderName;
    private String riderPhone;

    @Indexed
    private String driverId;         // driver profile id (Driver & Vehicle Service); null until assigned

    @Indexed
    private String driverAccountId;  // driver's account id, used for ownership checks

    private String driverName;
    private String driverPhone;
    private String vehicleId;
    private String vehicleNumber;

    private String serviceArea;
    private VehicleType vehicleType;

    private Location pickupLocation;
    private Location destinationLocation;

    @Indexed
    private RideStatus status;

    // Fare figures come from Fare & Payment Service, never from the client.
    private Double estimatedFare;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private Double finalFare;
    private Double actualDistanceKm;
    private Integer actualDurationMinutes;
    private String currency;
    private String paymentId;        // payment record id (Fare & Payment Service)

    private String cancellationReason;
    private String cancelledBy;      // "PASSENGER" | "DRIVER" | "ADMIN"

    // --- Lifecycle timestamps ---
    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version; // optimistic locking to avoid race conditions on status updates
}
