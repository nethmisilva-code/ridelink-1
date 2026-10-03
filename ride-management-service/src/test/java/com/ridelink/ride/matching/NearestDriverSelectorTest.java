package com.ridelink.ride.matching;

import com.ridelink.ride.client.dto.AvailableDriver;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NearestDriverSelectorTest {

    private static final Location PICKUP = Location.builder().address("Negombo Beach Road")
            .latitude(7.2087).longitude(79.8358).build();

    private final NearestDriverSelector selector = new NearestDriverSelector();

    @Test
    void ranksByDistanceToPickup_nearestFirst() {
        AvailableDriver far = driver("far", 7.1808, 79.8842, "2026-09-25T10:00:00Z");
        AvailableDriver near = driver("near", 7.2100, 79.8380, "2026-09-25T09:00:00Z");

        assertThat(selector.rank(List.of(far, near), PICKUP)).extracting(AvailableDriver::driverId)
                .containsExactly("near", "far");
    }

    @Test
    void equalDistance_prefersTheMostRecentLocationReport() {
        AvailableDriver older = driver("a-older", 7.2087, 79.8358, "2026-09-25T09:00:00Z");
        AvailableDriver newer = driver("b-newer", 7.2087, 79.8358, "2026-09-25T09:30:00Z");

        assertThat(selector.rank(List.of(older, newer), PICKUP).get(0).driverId()).isEqualTo("b-newer");
    }

    @Test
    void emptyCandidateList_givesEmptyRanking() {
        assertThat(selector.rank(List.of(), PICKUP)).isEmpty();
    }

    @Test
    void haversine_negomboBeachToAirport_isAbout6point2Kilometres() {
        assertThat(GeoDistance.haversineKm(7.2087, 79.8358, 7.1808, 79.8842)).isCloseTo(6.18, within(0.05));
        assertThat(GeoDistance.haversineKm(7.2087, 79.8358, 7.2087, 79.8358)).isEqualTo(0.0);
    }

    private static AvailableDriver driver(String id, double lat, double lng, String at) {
        return new AvailableDriver(id, "acc-" + id, "Negombo", lat, lng, Instant.parse(at), "v-" + id,
                "CAB-" + id, VehicleType.SEDAN, 4);
    }
}
