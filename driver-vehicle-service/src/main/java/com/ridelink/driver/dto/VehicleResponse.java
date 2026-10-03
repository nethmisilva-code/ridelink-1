package com.ridelink.driver.dto;

import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.model.VehicleType;

public record VehicleResponse(
        String id,
        String driverId,
        String plateNumber,
        String make,
        String model,
        int year,
        String colour,
        VehicleType vehicleType,
        int seats,
        boolean active
) {
    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(
                v.getId(), v.getDriverId(), v.getPlateNumber(), v.getMake(), v.getModel(),
                v.getYear(), v.getColour(), v.getVehicleType(), v.getSeats(), v.isActive());
    }
}
