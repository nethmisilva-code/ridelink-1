package com.ridelink.driver.service;

import com.ridelink.driver.client.AccountClient;
import com.ridelink.driver.client.AccountSummary;
import com.ridelink.driver.dto.AvailableDriverResponse;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.UpdateDriverRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.exception.BusinessRuleException;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.AvailabilityStatus;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.Location;
import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final AccountClient accountClient;
    private final Duration locationMaxAge;
    private final Clock clock;

    @Autowired
    public DriverService(DriverRepository driverRepository,
                         VehicleRepository vehicleRepository,
                         AccountClient accountClient,
                         @Value("${ridelink.matching.location-max-age-minutes:30}") long locationMaxAgeMinutes) {
        this(driverRepository, vehicleRepository, accountClient, Duration.ofMinutes(locationMaxAgeMinutes),
                Clock.systemUTC());
    }

    DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository,
                  AccountClient accountClient, Duration locationMaxAge, Clock clock) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.accountClient = accountClient;
        this.locationMaxAge = locationMaxAge;
        this.clock = clock;
    }

    /**
     * Synchronous interservice call to Account Service: a driver profile may
     * only be created for an account that exists, holds the DRIVER role and
     * has been approved (ACTIVE) by an administrator.
     */
    public Driver createProfile(CreateDriverRequest request, AuthenticatedUser caller, String bearerToken) {
        String accountId = resolveAccountId(request.accountId(), caller);
        if (driverRepository.existsByAccountId(accountId)) {
            throw new BusinessRuleException("A driver profile already exists for account " + accountId);
        }

        AccountSummary account = accountClient.fetchAccount(accountId, bearerToken);
        if (!"DRIVER".equalsIgnoreCase(account.role())) {
            throw new BusinessRuleException("Account " + accountId + " does not hold the DRIVER role");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new BusinessRuleException("Driver account " + accountId + " is " + account.status()
                    + ". An administrator must approve it (status ACTIVE) before a driver profile can be created");
        }

        Instant now = clock.instant();
        Driver driver = new Driver();
        driver.setAccountId(accountId);
        driver.setLicenseNumber(request.licenseNumber().trim());
        driver.setServiceArea(request.serviceArea().trim());
        driver.setAvailability(AvailabilityStatus.OFFLINE);
        driver.setCreatedAt(now);
        driver.setUpdatedAt(now);
        return driverRepository.save(driver);
    }

    public Driver getById(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
    }

    public Driver getMyProfile(AuthenticatedUser caller) {
        return driverRepository.findByAccountId(caller.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No driver profile exists for your account yet. Create one with POST /api/v1/drivers"));
    }

    public Driver updateProfile(String id, UpdateDriverRequest request, AuthenticatedUser caller) {
        Driver driver = getById(id);
        OwnershipGuard.assertCanManage(driver, caller, false);
        if (request.licenseNumber() != null) {
            driver.setLicenseNumber(request.licenseNumber().trim());
        }
        if (request.serviceArea() != null) {
            driver.setServiceArea(request.serviceArea().trim());
        }
        driver.setUpdatedAt(clock.instant());
        return driverRepository.save(driver);
    }

    /**
     * Availability rules:
     * AVAILABLE needs an active vehicle and a reported location;
     * ON_TRIP is set only by Ride Management Service (SERVICE role) or ADMIN, and only from AVAILABLE;
     * an ON_TRIP driver cannot go OFFLINE and is released by the ride service when the ride ends.
     */
    public Driver updateAvailability(String id, AvailabilityStatus target, AuthenticatedUser caller) {
        Driver driver = getById(id);
        OwnershipGuard.assertCanManage(driver, caller, true);
        AvailabilityStatus current = driver.getAvailability();

        if (current == target && target != AvailabilityStatus.ON_TRIP) {
            return driver;
        }

        // Business rule: a driver on a trip cannot go straight to OFFLINE.
        if (current == AvailabilityStatus.ON_TRIP && target == AvailabilityStatus.OFFLINE) {
            throw new BusinessRuleException("Cannot go OFFLINE while ON_TRIP");
        }

        if (target == AvailabilityStatus.ON_TRIP) {
            if (!caller.isPrivileged()) {
                throw new ForbiddenOperationException(
                        "ON_TRIP is set by Ride Management Service when a ride is assigned");
            }
            if (current != AvailabilityStatus.AVAILABLE) {
                throw new BusinessRuleException("Driver " + id + " is " + current + " and cannot be reserved for a ride");
            }
            // Atomic compare-and-set, so two rides can never reserve the same driver.
            Instant now = clock.instant();
            if (!driverRepository.reserveIfAvailable(id, now)) {
                throw new BusinessRuleException("Driver " + id + " was just reserved by another ride");
            }
            driver.setAvailability(AvailabilityStatus.ON_TRIP);
            driver.setUpdatedAt(now);
            return driver;
        }

        if (current == AvailabilityStatus.ON_TRIP && !caller.isPrivileged()) {
            throw new ForbiddenOperationException(
                    "You are on a trip. Availability is released automatically when the ride ends");
        }

        // Business rule: a driver cannot go AVAILABLE without an active vehicle and a location.
        if (target == AvailabilityStatus.AVAILABLE) {
            boolean hasVehicle = vehicleRepository.findByDriverId(id).stream().anyMatch(Vehicle::isActive);
            if (!hasVehicle) {
                throw new BusinessRuleException("Driver must have an active vehicle before becoming AVAILABLE");
            }
            if (driver.getCurrentLocation() == null) {
                throw new BusinessRuleException("Driver must report a current location before becoming AVAILABLE");
            }
        }

        driver.setAvailability(target);
        driver.setUpdatedAt(clock.instant());
        return driverRepository.save(driver);
    }

    public Driver updateLocation(String id, UpdateLocationRequest request, AuthenticatedUser caller) {
        Driver driver = getById(id);
        OwnershipGuard.assertCanManage(driver, caller, false);
        Instant now = clock.instant();
        driver.setCurrentLocation(new Location(request.latitude(), request.longitude()));
        driver.setLocationUpdatedAt(now);
        driver.setUpdatedAt(now);
        return driverRepository.save(driver);
    }

    /**
     * Called by Ride Management Service during driver assignment.
     * Eligible = AVAILABLE, same service area (case-insensitive), an active
     * vehicle of the requested type, and a location reported within the last
     * {@code ridelink.matching.location-max-age-minutes} minutes.
     */
    public List<AvailableDriverResponse> findEligibleDrivers(String serviceArea, VehicleType type) {
        if (serviceArea == null || serviceArea.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceArea must not be blank");
        }
        Instant freshAfter = clock.instant().minus(locationMaxAge);
        List<Driver> candidates = driverRepository
                .findByAvailabilityAndServiceAreaIgnoreCase(AvailabilityStatus.AVAILABLE, serviceArea.trim())
                .stream()
                .filter(d -> d.getCurrentLocation() != null && d.getLocationUpdatedAt() != null
                        && !d.getLocationUpdatedAt().isBefore(freshAfter))
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<String> ids = candidates.stream().map(Driver::getId).toList();
        Map<String, Vehicle> vehicleByDriver = vehicleRepository
                .findByDriverIdInAndVehicleTypeAndActiveTrue(ids, type).stream()
                .collect(Collectors.toMap(Vehicle::getDriverId, Function.identity(), (a, b) -> a));

        return candidates.stream()
                .filter(d -> vehicleByDriver.containsKey(d.getId()))
                .map(d -> {
                    Vehicle v = vehicleByDriver.get(d.getId());
                    return new AvailableDriverResponse(
                            d.getId(), d.getAccountId(), d.getServiceArea(),
                            d.getCurrentLocation().getLatitude(),
                            d.getCurrentLocation().getLongitude(),
                            d.getLocationUpdatedAt(),
                            v.getId(), v.getPlateNumber(), v.getVehicleType(), v.getSeats());
                })
                .toList();
    }

    private static String resolveAccountId(String requested, AuthenticatedUser caller) {
        boolean provided = requested != null && !requested.isBlank();
        if (caller.isDriver()) {
            if (provided && !requested.trim().equals(caller.id())) {
                throw new ForbiddenOperationException("Drivers can only create a profile for their own account");
            }
            return caller.id();
        }
        if (!provided) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "accountId is required when an administrator creates a driver profile");
        }
        return requested.trim();
    }
}
