package com.ridelink.ride.exception;

import com.ridelink.ride.model.RideStatus;

/** HTTP 409: the ride's current status does not allow the requested action. */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(RideStatus current, RideStatus target) {
        super(String.format("Invalid ride status transition: cannot move from %s to %s (allowed from %s: %s)",
                current, target, current, current.allowedTransitions().isEmpty() ? "none, it is final"
                        : current.allowedTransitions()));
    }
}
