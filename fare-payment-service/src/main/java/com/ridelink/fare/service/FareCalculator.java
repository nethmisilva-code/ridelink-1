package com.ridelink.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ridelink.fare.dto.Coordinates;
import com.ridelink.fare.dto.FareBreakdown;
import com.ridelink.fare.dto.RateCardEntry;
import com.ridelink.fare.model.VehicleType;

/**
 * The documented fare rule. Stateless and side-effect free, so it is used by
 * both the estimate (before the ride) and the final fare (after the ride).
 *
 * fare = baseFare + perKm x distanceKm + perMinute x durationMinutes   (LKR, rounded half-up to 2 decimals)
 *
 * When only coordinates are known, distance is the straight-line (haversine)
 * distance multiplied by a road factor of 1.3, and duration is that distance
 * driven at an average of 25 km/h, rounded up to whole minutes (minimum 1).
 */
@Component
public class FareCalculator {

    public static final String CURRENCY = "LKR";
    public static final BigDecimal ROAD_FACTOR = new BigDecimal("1.3");
    public static final BigDecimal AVERAGE_SPEED_KMH = new BigDecimal("25");
    public static final String RULE = "fare = base + perKm x distanceKm + perMinute x durationMinutes (LKR, 2 decimals). "
            + "Estimated distance = straight-line distance x 1.3; estimated duration = distance at 25 km/h, rounded up.";

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public FareBreakdown calculate(VehicleType type, BigDecimal distanceKm, int durationMinutes) {
        if (distanceKm.signum() < 0 || durationMinutes < 0) {
            throw new IllegalArgumentException("distance and duration cannot be negative");
        }
        BigDecimal distance = money(distanceKm);
        BigDecimal distanceCharge = money(type.perKm().multiply(distance));
        BigDecimal timeCharge = money(type.perMinute().multiply(BigDecimal.valueOf(durationMinutes)));
        BigDecimal base = money(type.baseFare());
        BigDecimal total = money(base.add(distanceCharge).add(timeCharge));
        return new FareBreakdown(type, distance, durationMinutes, base, distanceCharge, timeCharge, total, CURRENCY,
                RULE);
    }

    /** Simulated road distance between two points. */
    public BigDecimal roadDistanceKm(Coordinates from, Coordinates to) {
        double straightLine = haversineKm(from.latitude(), from.longitude(), to.latitude(), to.longitude());
        return BigDecimal.valueOf(straightLine).multiply(ROAD_FACTOR).setScale(2, RoundingMode.HALF_UP);
    }

    public int estimatedMinutes(BigDecimal distanceKm) {
        BigDecimal minutes = distanceKm.multiply(BigDecimal.valueOf(60))
                .divide(AVERAGE_SPEED_KMH, 0, RoundingMode.CEILING);
        return Math.max(1, minutes.intValueExact());
    }

    public List<RateCardEntry> rateCard() {
        return Arrays.stream(VehicleType.values())
                .map(t -> new RateCardEntry(t, t.baseFare(), t.perKm(), t.perMinute(), CURRENCY))
                .toList();
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
