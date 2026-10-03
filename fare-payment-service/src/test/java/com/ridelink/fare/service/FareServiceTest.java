package com.ridelink.fare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ridelink.fare.dto.Coordinates;
import com.ridelink.fare.dto.FareBreakdown;
import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.exception.BadRequestException;
import com.ridelink.fare.model.VehicleType;

class FareServiceTest {

    private final FareService fareService = new FareService(new FareCalculator());

    @Test
    void estimate_fromCoordinates_usesRoadDistanceAndEstimatedDuration() {
        FareBreakdown fare = fareService.estimate(new FareEstimateRequest(new Coordinates(7.2087, 79.8358),
                new Coordinates(7.1808, 79.8842), null, VehicleType.SEDAN));

        assertThat(fare.distanceKm()).isGreaterThan(new BigDecimal("7"));
        assertThat(fare.estimatedDurationMinutes()).isBetween(17, 20);
        assertThat(fare.totalFare()).isBetween(new BigDecimal("1200"), new BigDecimal("1350"));
    }

    @Test
    void estimate_fromDistance_takesPrecedenceOverCoordinates() {
        FareBreakdown fare = fareService.estimate(new FareEstimateRequest(null, null, new BigDecimal("10"),
                VehicleType.TUK));

        // 100 + 80*10 + 5*24
        assertThat(fare.totalFare()).isEqualByComparingTo("1020.00");
    }

    @Test
    void estimate_withoutDistanceOrBothPoints_isABadRequest() {
        assertThatThrownBy(() -> fareService.estimate(new FareEstimateRequest(new Coordinates(7.2, 79.8), null, null,
                VehicleType.SEDAN))).isInstanceOf(BadRequestException.class);
    }
}
