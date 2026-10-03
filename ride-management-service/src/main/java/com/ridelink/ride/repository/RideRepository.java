package com.ridelink.ride.repository;

import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {

    // Rides for a specific rider/user, newest first
    List<Ride> findByRiderIdOrderByCreatedAtDesc(String riderId);

    // Rides for a specific driver profile, newest first
    List<Ride> findByDriverIdOrderByCreatedAtDesc(String driverId);

    // Rides for the driver's account (used by GET /rides/me for drivers)
    List<Ride> findByDriverAccountIdOrderByCreatedAtDesc(String driverAccountId);

    // Rides in any of the given statuses (used for "active/pending" listing)
    List<Ride> findByStatusInOrderByCreatedAtDesc(Collection<RideStatus> statuses);

    boolean existsByRiderIdAndStatusIn(String riderId, Collection<RideStatus> statuses);

    boolean existsByDriverIdAndStatusIn(String driverId, Collection<RideStatus> statuses);
}
