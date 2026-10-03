package com.ridelink.fare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.fare.dto.FareBreakdown;
import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FinalFareRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.dto.RateCardResponse;
import com.ridelink.fare.service.FareService;
import com.ridelink.fare.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fares")
@Tag(name = "Fares", description = "Rate card, fare estimation and final fare calculation")
public class FareController {

    private final FareService fareService;
    private final PaymentService paymentService;

    public FareController(FareService fareService, PaymentService paymentService) {
        this.fareService = fareService;
        this.paymentService = paymentService;
    }

    @GetMapping("/rates")
    @Operation(summary = "Rate card and fare rule (public)")
    public RateCardResponse rates() {
        return fareService.rateCard();
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate a fare from pickup and destination coordinates (or a distance)",
            description = "Also called by Ride Management Service when a ride is requested.")
    @ApiResponse(responseCode = "200", description = "Fare estimate with breakdown")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public FareBreakdown estimate(@Valid @RequestBody FareEstimateRequest request) {
        return fareService.estimate(request);
    }

    @PostMapping("/final")
    @PreAuthorize("hasAnyRole('SERVICE','ADMIN')")
    @Operation(summary = "Calculate the final fare and open a PENDING payment (Ride Management Service only)",
            description = "Idempotent per ride: 201 the first time, 200 with the same payment on a retry.")
    @ApiResponse(responseCode = "201", description = "Pending payment created")
    @ApiResponse(responseCode = "200", description = "Already calculated for this ride")
    public ResponseEntity<PaymentResponse> finalFare(@Valid @RequestBody FinalFareRequest request) {
        PaymentService.FinalFareResult result = paymentService.createFinalFare(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.payment());
    }
}
