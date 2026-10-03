package com.ridelink.fare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.ridelink.fare.dto.Coordinates;
import com.ridelink.fare.dto.FareBreakdown;
import com.ridelink.fare.model.VehicleType;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator();

    @ParameterizedTest(name = "{0}: {1} km, {2} min = LKR {3}")
    @CsvSource({
            "TUK,   5,     12, 560.00",   // 100 + 80*5 + 5*12
            "SEDAN, 10,    20, 1560.00",  // 200 + 120*10 + 8*20
            "VAN,   10,    20, 2000.00",  // 300 + 150*10 + 10*20
            "SUV,   10,    20, 2290.00",  // 350 + 170*10 + 12*20
            "SEDAN, 3.333, 1,  607.60"    // distance rounded to 3.33 first: 200 + 399.60 + 8
    })
    void appliesTheDocumentedRule(VehicleType type, String km, int minutes, String expected) {
        FareBreakdown fare = calculator.calculate(type, new BigDecimal(km), minutes);

        assertThat(fare.totalFare()).isEqualByComparingTo(expected);
        assertThat(fare.totalFare().add(BigDecimal.ZERO))
                .isEqualByComparingTo(fare.baseFare().add(fare.distanceCharge()).add(fare.timeCharge()));
        assertThat(fare.currency()).isEqualTo("LKR");
    }

    @ParameterizedTest
    @EnumSource(VehicleType.class)
    void zeroDistanceAndTime_costsExactlyTheBaseFare(VehicleType type) {
        assertThat(calculator.calculate(type, BigDecimal.ZERO, 0).totalFare()).isEqualByComparingTo(type.baseFare());
    }

    @Test
    void negativeInput_isRejected() {
        assertThatThrownBy(() -> calculator.calculate(VehicleType.TUK, new BigDecimal("-1"), 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roadDistance_isStraightLineTimesRoadFactor() {
        BigDecimal km = calculator.roadDistanceKm(new Coordinates(7.2087, 79.8358), new Coordinates(7.1808, 79.8842));

        // about 6.18 km straight line x 1.3 = 8.03 km
        assertThat(km).isEqualByComparingTo("8.03");
        assertThat(km.scale()).isEqualTo(2);
    }

    @Test
    void estimatedMinutes_assume25KmhRoundedUp_withAMinimumOfOne() {
        assertThat(calculator.estimatedMinutes(new BigDecimal("10"))).isEqualTo(24);
        assertThat(calculator.estimatedMinutes(new BigDecimal("7.67"))).isEqualTo(19);
        assertThat(calculator.estimatedMinutes(BigDecimal.ZERO)).isEqualTo(1);
    }

    @Test
    void rateCard_listsEveryVehicleType() {
        assertThat(calculator.rateCard()).hasSize(VehicleType.values().length);
    }
}
