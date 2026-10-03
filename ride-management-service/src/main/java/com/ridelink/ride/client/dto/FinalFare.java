package com.ridelink.ride.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** The parts of the pending payment record returned by POST /api/v1/fares/final. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinalFare(
        String id,
        String rideId,
        BigDecimal amount,
        String currency,
        String status) {
}
