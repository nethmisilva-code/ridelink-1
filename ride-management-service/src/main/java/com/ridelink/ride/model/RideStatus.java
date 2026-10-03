package com.ridelink.ride.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle state of a Ride and the allowed transitions:
 *
 *   REQUESTED   -> ASSIGNED, CANCELLED
 *   ASSIGNED    -> ACCEPTED, REQUESTED (driver declines), CANCELLED
 *   ACCEPTED    -> IN_PROGRESS, CANCELLED
 *   IN_PROGRESS -> COMPLETED       (a started trip must be completed so it can be charged)
 *   COMPLETED   -> (terminal)
 *   CANCELLED   -> (terminal)
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /** Statuses in which a ride still occupies its passenger (and driver, once assigned). */
    public static final Set<RideStatus> ACTIVE = Collections.unmodifiableSet(
            EnumSet.of(REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS));

    private static final Map<RideStatus, Set<RideStatus>> ALLOWED = new EnumMap<>(RideStatus.class);

    static {
        ALLOWED.put(REQUESTED, EnumSet.of(ASSIGNED, CANCELLED));
        ALLOWED.put(ASSIGNED, EnumSet.of(ACCEPTED, REQUESTED, CANCELLED));
        ALLOWED.put(ACCEPTED, EnumSet.of(IN_PROGRESS, CANCELLED));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(COMPLETED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(RideStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(RideStatus.class));
    }

    public boolean canTransitionTo(RideStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public Set<RideStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }
}
