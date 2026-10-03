package com.ridelink.driver.repository;

import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.model.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    /** All vehicles belonging to one driver, active or not. Used by the driver's own vehicle listing. */
    List<Vehicle> findByDriverId(String driverId);

    /** Only the usable vehicles. Used when deciding whether a driver may go AVAILABLE. */
    List<Vehicle> findByDriverIdAndActiveTrue(String driverId);

    /**
     * Batched lookup for the eligible-driver search. One query for every candidate driver
     * instead of one query per driver, which keeps /api/v1/drivers/available O(2) queries.
     */
    List<Vehicle> findByDriverIdInAndVehicleTypeAndActiveTrue(List<String> driverIds, VehicleType vehicleType);

    /** Guard for the "plate already registered" business rule. */
    boolean existsByPlateNumber(String plateNumber);

    /** Used when updating a vehicle, so a driver cannot take a plate that belongs to someone else. */
    Optional<Vehicle> findByPlateNumber(String plateNumber);

    /** Counts remaining usable vehicles, for the "cannot deactivate the last active vehicle" rule. */
    long countByDriverIdAndActiveTrue(String driverId);
}