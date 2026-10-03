package com.ridelink.ride.service.impl;

import com.ridelink.ride.client.DriverClient;
import com.ridelink.ride.client.FareClient;
import com.ridelink.ride.client.dto.AvailableDriver;
import com.ridelink.ride.client.dto.FareQuote;
import com.ridelink.ride.client.dto.FinalFare;
import com.ridelink.ride.client.dto.FinalFareRequest;
import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.LocationDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.DownstreamServiceException;
import com.ridelink.ride.exception.InvalidRequestException;
import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.exception.NoDriverAvailableException;
import com.ridelink.ride.exception.RideAccessDeniedException;
import com.ridelink.ride.exception.RideConflictException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.matching.GeoDistance;
import com.ridelink.ride.matching.NearestDriverSelector;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.AuthenticatedUser;
import com.ridelink.ride.service.RideService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private static final Logger log = LoggerFactory.getLogger(RideServiceImpl.class);

    /** Pickup and destination closer than this are treated as the same place. */
    static final double MIN_TRIP_KM = 0.05;

    private final RideRepository rideRepository;
    private final DriverClient driverClient;
    private final FareClient fareClient;
    private final NearestDriverSelector driverSelector;
    private final Clock clock;

    // ------------------------------------------------------------------
    // 1. Ride request creation (calls Fare & Payment Service for the estimate)
    // ------------------------------------------------------------------
    @Override
    public RideResponse createRide(CreateRideRequest request, AuthenticatedUser caller) {
        String riderId = caller.id();
        // Business rule: a rider cannot have more than one active ride at a time
        if (rideRepository.existsByRiderIdAndStatusIn(riderId, RideStatus.ACTIVE)) {
            throw new RideConflictException("Rider " + riderId + " already has an active ride. "
                    + "Complete or cancel it before requesting another.");
        }

        Location pickup = toEntity(request.getPickupLocation());
        Location destination = toEntity(request.getDestinationLocation());
        double straightLineKm = GeoDistance.haversineKm(pickup.getLatitude(), pickup.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
        if (straightLineKm < MIN_TRIP_KM) {
            throw new InvalidRequestException("Pickup and destination must be different places");
        }

        FareQuote quote = fareClient.estimate(pickup, destination, request.getVehicleType());

        Ride ride = Ride.builder()
                .riderId(riderId)
                .riderName(trimToNull(request.getRiderName()))
                .riderPhone(trimToNull(request.getRiderPhone()))
                .serviceArea(request.getServiceArea().trim())
                .vehicleType(request.getVehicleType())
                .pickupLocation(pickup)
                .destinationLocation(destination)
                .status(RideStatus.REQUESTED)
                .estimatedFare(quote.totalFare().doubleValue())
                .estimatedDistanceKm(quote.distanceKm() == null ? null : quote.distanceKm().doubleValue())
                .estimatedDurationMinutes(quote.estimatedDurationMinutes())
                .currency(quote.currency())
                .requestedAt(clock.instant())
                .build();

        return RideResponse.fromEntity(rideRepository.save(ride));
    }

    // ------------------------------------------------------------------
    // 2. Driver assignment (calls Driver & Vehicle Service)
    // ------------------------------------------------------------------
    @Override
    public RideResponse assignDriver(String rideId, AssignDriverRequest request, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        if (!caller.isAdmin() && !isRider(ride, caller)) {
            throw new RideAccessDeniedException("Only the passenger who requested this ride can ask for a driver");
        }
        validateTransition(ride.getStatus(), RideStatus.ASSIGNED);

        String requestedDriverId = request == null ? null : trimToNull(request.getDriverId());
        if (requestedDriverId != null && !caller.isAdmin()) {
            throw new RideAccessDeniedException("Only an administrator can choose a specific driver");
        }

        List<AvailableDriver> candidates = driverClient.findAvailableDrivers(ride.getServiceArea(), ride.getVehicleType())
                .stream()
                .filter(d -> requestedDriverId == null || requestedDriverId.equals(d.driverId()))
                .filter(d -> !rideRepository.existsByDriverIdAndStatusIn(d.driverId(), RideStatus.ACTIVE))
                .toList();

        for (AvailableDriver candidate : driverSelector.rank(candidates, ride.getPickupLocation())) {
            if (driverClient.reserve(candidate.driverId())) {
                return RideResponse.fromEntity(saveAssignment(ride, candidate));
            }
            log.info("Driver {} was no longer available for ride {}, trying the next nearest", candidate.driverId(),
                    rideId);
        }

        throw new NoDriverAvailableException(requestedDriverId != null
                ? "Driver " + requestedDriverId + " is not eligible for this ride right now"
                : "No available " + ride.getVehicleType() + " driver in " + ride.getServiceArea()
                        + " right now. The ride stays REQUESTED: try again shortly or cancel it.");
    }

    private Ride saveAssignment(Ride ride, AvailableDriver driver) {
        ride.setDriverId(driver.driverId());
        ride.setDriverAccountId(driver.accountId());
        ride.setVehicleId(driver.vehicleId());
        ride.setVehicleNumber(driver.plateNumber());
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(clock.instant());
        try {
            return rideRepository.save(ride);
        }
        catch (OptimisticLockingFailureException ex) {
            // The ride changed concurrently (e.g. cancelled): undo the reservation.
            releaseDriverQuietly(driver.driverId(), ride.getId());
            throw ex;
        }
    }

    @Override
    public RideResponse acceptRide(String rideId, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        assertAssignedDriver(ride, caller);
        validateTransition(ride.getStatus(), RideStatus.ACCEPTED);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(clock.instant());
        return RideResponse.fromEntity(rideRepository.save(ride));
    }

    /** The assigned driver declines: the ride goes back to REQUESTED and the driver is released. */
    @Override
    public RideResponse declineRide(String rideId, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        assertAssignedDriver(ride, caller);
        validateTransition(ride.getStatus(), RideStatus.REQUESTED);
        String driverId = ride.getDriverId();
        ride.setDriverId(null);
        ride.setDriverAccountId(null);
        ride.setVehicleId(null);
        ride.setVehicleNumber(null);
        ride.setAssignedAt(null);
        ride.setStatus(RideStatus.REQUESTED);
        Ride saved = rideRepository.save(ride);
        releaseDriverQuietly(driverId, rideId);
        return RideResponse.fromEntity(saved);
    }

    // ------------------------------------------------------------------
    // 3. Ride status lifecycle
    // ------------------------------------------------------------------
    @Override
    public RideResponse startRide(String rideId, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        assertAssignedDriver(ride, caller);
        validateTransition(ride.getStatus(), RideStatus.IN_PROGRESS);
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(clock.instant());
        return RideResponse.fromEntity(rideRepository.save(ride));
    }

    /**
     * Completes the ride: Fare &amp; Payment Service calculates the final fare and
     * opens a PENDING payment. If that call fails the ride stays IN_PROGRESS,
     * so a ride is never completed without a fare.
     */
    @Override
    public RideResponse completeRide(String rideId, CompleteRideRequest request, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        assertAssignedDriver(ride, caller);
        validateTransition(ride.getStatus(), RideStatus.COMPLETED);

        Instant now = clock.instant();
        double distanceKm = request != null && request.getActualDistanceKm() != null
                ? request.getActualDistanceKm()
                : Objects.requireNonNullElse(ride.getEstimatedDistanceKm(), 1.0);
        int durationMinutes = billableMinutes(ride.getStartedAt(), now);

        FinalFare fare = fareClient.calculateFinalFare(new FinalFareRequest(ride.getId(), ride.getRiderId(),
                ride.getDriverId(), ride.getDriverAccountId(), ride.getVehicleType(), distanceKm, durationMinutes));

        ride.setActualDistanceKm(distanceKm);
        ride.setActualDurationMinutes(durationMinutes);
        ride.setFinalFare(fare.amount().doubleValue());
        ride.setCurrency(fare.currency());
        ride.setPaymentId(fare.id());
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        Ride saved = rideRepository.save(ride);
        releaseDriverQuietly(saved.getDriverId(), rideId);
        return RideResponse.fromEntity(saved);
    }

    @Override
    public RideResponse cancelRide(String rideId, CancelRideRequest request, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        if (!caller.isAdmin() && !isRider(ride, caller) && !isAssignedDriver(ride, caller)) {
            throw new RideAccessDeniedException("Only the passenger, the assigned driver or an administrator "
                    + "can cancel this ride");
        }
        validateTransition(ride.getStatus(), RideStatus.CANCELLED);

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(clock.instant());
        ride.setCancelledBy(caller.role());
        ride.setCancellationReason(request == null ? null : trimToNull(request.getReason()));
        Ride saved = rideRepository.save(ride);
        if (saved.getDriverId() != null) {
            releaseDriverQuietly(saved.getDriverId(), rideId);
        }
        return RideResponse.fromEntity(saved);
    }

    // ------------------------------------------------------------------
    // 4. Ride retrieval
    // ------------------------------------------------------------------
    @Override
    public RideResponse getRideById(String rideId, AuthenticatedUser caller) {
        Ride ride = getRideOrThrow(rideId);
        if (!caller.isPrivileged() && !isRider(ride, caller) && !isAssignedDriver(ride, caller)) {
            throw new RideAccessDeniedException("You can only view rides you are part of");
        }
        return RideResponse.fromEntity(ride);
    }

    @Override
    public List<RideResponse> getMyRides(AuthenticatedUser caller) {
        if (caller.isDriver()) {
            return toResponses(rideRepository.findByDriverAccountIdOrderByCreatedAtDesc(caller.id()));
        }
        if (caller.isPassenger()) {
            return toResponses(rideRepository.findByRiderIdOrderByCreatedAtDesc(caller.id()));
        }
        return getAllRides();
    }

    @Override
    public List<RideResponse> getAllRides() {
        return toResponses(rideRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    public List<RideResponse> getRidesByUserId(String userId, AuthenticatedUser caller) {
        if (!caller.isAdmin() && !caller.id().equals(userId)) {
            throw new RideAccessDeniedException("You can only list your own rides");
        }
        return toResponses(rideRepository.findByRiderIdOrderByCreatedAtDesc(userId));
    }

    @Override
    public List<RideResponse> getRidesByDriverId(String driverId) {
        return toResponses(rideRepository.findByDriverIdOrderByCreatedAtDesc(driverId));
    }

    @Override
    public List<RideResponse> getActiveRides() {
        return toResponses(rideRepository.findByStatusInOrderByCreatedAtDesc(RideStatus.ACTIVE));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Ride getRideOrThrow(String rideId) {
        String requiredRideId = Objects.requireNonNull(rideId, "rideId must not be null");
        return rideRepository.findById(requiredRideId)
                .orElseThrow(() -> new RideNotFoundException(requiredRideId));
    }

    private static void validateTransition(RideStatus current, RideStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
    }

    private static boolean isRider(Ride ride, AuthenticatedUser caller) {
        return caller.isPassenger() && caller.id().equals(ride.getRiderId());
    }

    private static boolean isAssignedDriver(Ride ride, AuthenticatedUser caller) {
        return caller.isDriver() && caller.id().equals(ride.getDriverAccountId());
    }

    private static void assertAssignedDriver(Ride ride, AuthenticatedUser caller) {
        if (!caller.isAdmin() && !isAssignedDriver(ride, caller)) {
            throw new RideAccessDeniedException("Only the driver assigned to this ride can do this");
        }
    }

    /** Whole minutes from start to completion, rounded up, minimum 1. */
    static int billableMinutes(Instant startedAt, Instant completedAt) {
        if (startedAt == null || !completedAt.isAfter(startedAt)) {
            return 1;
        }
        long seconds = Duration.between(startedAt, completedAt).toSeconds();
        return (int) Math.max(1, (seconds + 59) / 60);
    }

    /**
     * Releasing a driver is best effort: the ride state change has already been
     * saved and must not be rolled back if Driver &amp; Vehicle Service is down.
     */
    private void releaseDriverQuietly(String driverId, String rideId) {
        if (driverId == null) {
            return;
        }
        try {
            driverClient.release(driverId);
        }
        catch (DownstreamServiceException ex) {
            log.warn("Ride {}: driver {} could not be set back to AVAILABLE ({}). "
                    + "An administrator can reset the driver's availability.", rideId, driverId, ex.getMessage());
        }
    }

    private static List<RideResponse> toResponses(List<Ride> rides) {
        return rides.stream().map(RideResponse::fromEntity).toList();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Location toEntity(LocationDto dto) {
        return Location.builder()
                .address(dto.getAddress().trim())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .landmark(trimToNull(dto.getLandmark()))
                .build();
    }
}
