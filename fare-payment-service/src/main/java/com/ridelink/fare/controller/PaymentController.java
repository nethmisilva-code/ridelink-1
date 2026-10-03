package com.ridelink.fare.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.security.AuthenticatedUser;
import com.ridelink.fare.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Simulated payments, payment status and receipts")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    @Operation(summary = "Pay for a completed ride (simulated)",
            description = "CASH and WALLET always succeed. CARD succeeds unless the test card number ends in 0002.")
    @ApiResponse(responseCode = "200", description = "Payment SUCCESS")
    @ApiResponse(responseCode = "402", description = "Payment FAILED (declined); it can be retried")
    @ApiResponse(responseCode = "404", description = "Ride has not been completed yet")
    @ApiResponse(responseCode = "409", description = "Ride already paid")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        PaymentResponse payment = paymentService.pay(request, AuthenticatedUser.from(authentication));
        HttpStatus status = payment.status() == PaymentStatus.SUCCESS ? HttpStatus.OK : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(payment);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Payment status for a ride (passenger, driver of the ride, ADMIN)")
    public PaymentResponse forRide(@PathVariable String rideId, Authentication authentication) {
        return paymentService.getForRide(rideId, AuthenticatedUser.from(authentication));
    }

    @GetMapping("/ride/{rideId}/receipt")
    @Operation(summary = "Receipt for a paid ride (passenger, driver of the ride, ADMIN)")
    @ApiResponse(responseCode = "409", description = "Ride not paid yet")
    public ReceiptResponse receipt(@PathVariable String rideId, Authentication authentication) {
        return paymentService.getReceipt(rideId, AuthenticatedUser.from(authentication));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment by id")
    public PaymentResponse byId(@PathVariable String paymentId, Authentication authentication) {
        return paymentService.getById(paymentId, AuthenticatedUser.from(authentication));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List payments, optionally by status (ADMIN)")
    public List<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.list(status);
    }
}
