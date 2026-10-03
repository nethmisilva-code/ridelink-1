package com.ridelink.ride.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded (not a separate collection) document representing a geo point + address.
 * Stored inline inside the Ride document for both pickup and destination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    private String address;
    private Double latitude;
    private Double longitude;
    private String landmark; // optional extra detail, e.g. "Near City Mall"
}
