package com.ridelink.ride.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RideStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "REQUESTED, ASSIGNED", "REQUESTED, CANCELLED",
            "ASSIGNED, ACCEPTED", "ASSIGNED, REQUESTED", "ASSIGNED, CANCELLED",
            "ACCEPTED, IN_PROGRESS", "ACCEPTED, CANCELLED",
            "IN_PROGRESS, COMPLETED"
    })
    void allowedTransitions(RideStatus from, RideStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "REQUESTED, ACCEPTED", "REQUESTED, IN_PROGRESS", "REQUESTED, COMPLETED",
            "ASSIGNED, IN_PROGRESS", "ACCEPTED, COMPLETED", "IN_PROGRESS, CANCELLED",
            "COMPLETED, CANCELLED", "CANCELLED, REQUESTED", "COMPLETED, COMPLETED"
    })
    void rejectedTransitions(RideStatus from, RideStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void completedAndCancelled_areTerminal_andNotActive() {
        assertThat(RideStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(RideStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(RideStatus.ACTIVE).containsExactlyInAnyOrder(
                RideStatus.REQUESTED, RideStatus.ASSIGNED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS);
    }
}
