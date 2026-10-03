package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.model.VehicleType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        String id,
        String rideId,
        String passengerId,
        String driverId,
        String driverAccountId,
        VehicleType vehicleType,
        BigDecimal distanceKm,
        Integer durationMinutes,
        BigDecimal baseFare,
        BigDecimal distanceCharge,
        BigDecimal timeCharge,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        String cardLast4,
        String transactionId,
        String receiptNumber,
        String failureReason,
        int attempts,
        Instant createdAt,
        Instant paidAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getRideId(), p.getPassengerId(), p.getDriverId(),
                p.getDriverAccountId(), p.getVehicleType(), p.getDistanceKm(), p.getDurationMinutes(),
                p.getBaseFare(), p.getDistanceCharge(), p.getTimeCharge(), p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getPaymentMethod(), p.getCardLast4(), p.getTransactionId(), p.getReceiptNumber(),
                p.getFailureReason(), p.getAttempts(), p.getCreatedAt(), p.getPaidAt());
    }
}
