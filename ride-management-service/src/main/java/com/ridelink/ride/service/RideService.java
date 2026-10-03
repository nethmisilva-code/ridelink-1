package com.ridelink.ride.service;

import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.security.AuthenticatedUser;

import java.util.List;

public interface RideService {

    RideResponse createRide(CreateRideRequest request, AuthenticatedUser caller);

    RideResponse assignDriver(String rideId, AssignDriverRequest request, AuthenticatedUser caller);

    RideResponse acceptRide(String rideId, AuthenticatedUser caller);

    RideResponse declineRide(String rideId, AuthenticatedUser caller);

    RideResponse startRide(String rideId, AuthenticatedUser caller);

    RideResponse completeRide(String rideId, CompleteRideRequest request, AuthenticatedUser caller);

    RideResponse cancelRide(String rideId, CancelRideRequest request, AuthenticatedUser caller);

    RideResponse getRideById(String rideId, AuthenticatedUser caller);

    List<RideResponse> getMyRides(AuthenticatedUser caller);

    List<RideResponse> getAllRides();

    List<RideResponse> getRidesByUserId(String userId, AuthenticatedUser caller);

    List<RideResponse> getRidesByDriverId(String driverId);

    List<RideResponse> getActiveRides();
}
