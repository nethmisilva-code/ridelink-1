package com.ridelink.ride.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideResponse {

    private String id;
    private String riderId;
    private String riderName;
    private String riderPhone;

    private String driverId;
    private String driverAccountId;
    private String driverName;
    private String driverPhone;
    private String vehicleId;
    private String vehicleNumber;

    private String serviceArea;
    private VehicleType vehicleType;
    private Location pickupLocation;
    private Location destinationLocation;

    private RideStatus status;

    private Double estimatedFare;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private Double finalFare;
    private Double actualDistanceKm;
    private Integer actualDurationMinutes;
    private String currency;
    private String paymentId;

    private String cancellationReason;
    private String cancelledBy;

    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static RideResponse fromEntity(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .riderId(ride.getRiderId())
                .riderName(ride.getRiderName())
                .riderPhone(ride.getRiderPhone())
                .driverId(ride.getDriverId())
                .driverAccountId(ride.getDriverAccountId())
                .driverName(ride.getDriverName())
                .driverPhone(ride.getDriverPhone())
                .vehicleId(ride.getVehicleId())
                .vehicleNumber(ride.getVehicleNumber())
                .serviceArea(ride.getServiceArea())
                .vehicleType(ride.getVehicleType())
                .pickupLocation(ride.getPickupLocation())
                .destinationLocation(ride.getDestinationLocation())
                .status(ride.getStatus())
                .estimatedFare(ride.getEstimatedFare())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                .finalFare(ride.getFinalFare())
                .actualDistanceKm(ride.getActualDistanceKm())
                .actualDurationMinutes(ride.getActualDurationMinutes())
                .currency(ride.getCurrency())
                .paymentId(ride.getPaymentId())
                .cancellationReason(ride.getCancellationReason())
                .cancelledBy(ride.getCancelledBy())
                .requestedAt(ride.getRequestedAt())
                .assignedAt(ride.getAssignedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}
