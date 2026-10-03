package com.ridelink.ride.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** The parts of the Fare &amp; Payment estimate response this service stores on the ride. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FareQuote(
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal totalFare,
        String currency) {
}
