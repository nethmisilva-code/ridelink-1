package com.ridelink.fare.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.ridelink.fare.dto.FareBreakdown;
import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.RateCardResponse;
import com.ridelink.fare.exception.BadRequestException;

@Service
public class FareService {

    private final FareCalculator calculator;

    public FareService(FareCalculator calculator) {
        this.calculator = calculator;
    }

    public FareBreakdown estimate(FareEstimateRequest request) {
        BigDecimal distance;
        if (request.distanceKm() != null) {
            distance = request.distanceKm();
        }
        else if (request.pickup() != null && request.destination() != null) {
            distance = calculator.roadDistanceKm(request.pickup(), request.destination());
        }
        else {
            throw new BadRequestException("Provide either distanceKm or both pickup and destination coordinates");
        }
        return calculator.calculate(request.vehicleType(), distance, calculator.estimatedMinutes(distance));
    }

    public RateCardResponse rateCard() {
        return new RateCardResponse(FareCalculator.RULE, calculator.rateCard());
    }
}
