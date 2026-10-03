package com.ridelink.fare.dto;

import java.util.List;

public record RateCardResponse(String rule, List<RateCardEntry> rates) {
}
