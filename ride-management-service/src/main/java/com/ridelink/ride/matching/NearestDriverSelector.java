package com.ridelink.ride.matching;

import com.ridelink.ride.client.dto.AvailableDriver;
import com.ridelink.ride.model.Location;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Documented driver-selection rule:
 * candidates (already filtered by Driver &amp; Vehicle Service to AVAILABLE drivers
 * in the service area with an active vehicle of the right type and a fresh
 * location) are ranked by straight-line distance to the pickup point, nearest
 * first. Ties go to the driver whose location was reported most recently,
 * then to the lowest driver id so the result is deterministic.
 */
@Component
public class NearestDriverSelector {

    public List<AvailableDriver> rank(List<AvailableDriver> candidates, Location pickup) {
        Comparator<AvailableDriver> byDistance = Comparator.comparingDouble(d -> distanceKm(d, pickup));
        Comparator<AvailableDriver> byFreshness = Comparator.comparing(
                (AvailableDriver d) -> d.locationUpdatedAt() == null ? Instant.EPOCH : d.locationUpdatedAt())
                .reversed();
        return candidates.stream()
                .sorted(byDistance.thenComparing(byFreshness).thenComparing(AvailableDriver::driverId))
                .toList();
    }

    public static double distanceKm(AvailableDriver driver, Location pickup) {
        return GeoDistance.haversineKm(driver.latitude(), driver.longitude(),
                pickup.getLatitude(), pickup.getLongitude());
    }
}
