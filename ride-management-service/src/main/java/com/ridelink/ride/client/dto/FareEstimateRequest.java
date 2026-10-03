package com.ridelink.ride.client.dto;

import com.ridelink.ride.model.VehicleType;

/** Request contract of Fare &amp; Payment Service POST /api/v1/fares/estimate. */
public record FareEstimateRequest(Coordinates pickup, Coordinates destination, VehicleType vehicleType) {
}
