package com.ridelink.driver.service;

import com.ridelink.driver.dto.CreateVehicleRequest;
import com.ridelink.driver.dto.UpdateVehicleRequest;
import com.ridelink.driver.exception.BusinessRuleException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public Vehicle createVehicle(CreateVehicleRequest request, AuthenticatedUser caller) {
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.driverId()));
        OwnershipGuard.assertCanManage(driver, caller, false);
        assertValidYear(request.year());

        String plate = normalisePlate(request.plateNumber());
        if (vehicleRepository.existsByPlateNumber(plate)) {
            throw new BusinessRuleException("Plate number already registered: " + plate);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setDriverId(request.driverId());
        vehicle.setPlateNumber(plate);
        vehicle.setMake(request.make().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setYear(request.year());
        vehicle.setColour(request.colour().trim());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setSeats(request.seats());
        vehicle.setActive(true);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle getById(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    public List<Vehicle> listByDriver(String driverId) {
        return vehicleRepository.findByDriverId(driverId);
    }

    public Vehicle updateDetails(String id, UpdateVehicleRequest request, AuthenticatedUser caller) {
        Vehicle vehicle = getById(id);
        assertOwnsVehicle(vehicle, caller);
        assertValidYear(request.year());
        vehicle.setMake(request.make().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setYear(request.year());
        vehicle.setColour(request.colour().trim());
        vehicle.setSeats(request.seats());
        vehicle.setUpdatedAt(Instant.now());
        return vehicleRepository.save(vehicle);
    }

    /**
     * Business rule: a driver can never be left with zero active vehicles by
     * deactivating one, since DriverService requires an active vehicle before
     * a driver may go AVAILABLE.
     */
    public Vehicle setActive(String id, boolean active, AuthenticatedUser caller) {
        Vehicle vehicle = getById(id);
        assertOwnsVehicle(vehicle, caller);

        if (!active && vehicle.isActive()) {
            long remainingActive = vehicleRepository.countByDriverIdAndActiveTrue(vehicle.getDriverId());
            if (remainingActive <= 1) {
                throw new BusinessRuleException(
                        "Cannot deactivate the last active vehicle for driver " + vehicle.getDriverId());
            }
        }

        vehicle.setActive(active);
        vehicle.setUpdatedAt(Instant.now());
        return vehicleRepository.save(vehicle);
    }

    private void assertOwnsVehicle(Vehicle vehicle, AuthenticatedUser caller) {
        Driver driver = driverRepository.findById(vehicle.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + vehicle.getDriverId()));
        OwnershipGuard.assertCanManage(driver, caller, false);
    }

    private static void assertValidYear(int year) {
        int latest = Year.now().getValue() + 1;
        if (year > latest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year must be " + latest + " or earlier");
        }
    }

    /** "cab-1234 " and "CAB-1234" are the same plate. */
    static String normalisePlate(String plate) {
        return plate.trim().toUpperCase(Locale.ROOT);
    }
}
