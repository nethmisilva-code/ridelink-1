package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.model.VehicleType;
import com.ridelink.fare.service.FareCalculator;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiptResponse(
        String receiptNumber,
        String paymentId,
        String transactionId,
        String rideId,
        String passengerId,
        String driverId,
        VehicleType vehicleType,
        BigDecimal distanceKm,
        Integer durationMinutes,
        BigDecimal baseFare,
        BigDecimal distanceCharge,
        BigDecimal timeCharge,
        BigDecimal totalAmount,
        String currency,
        PaymentMethod paymentMethod,
        String maskedCard,
        PaymentStatus status,
        Instant paidAt,
        String fareRule) {

    public static ReceiptResponse from(Payment p) {
        return new ReceiptResponse(p.getReceiptNumber(), p.getId(), p.getTransactionId(), p.getRideId(),
                p.getPassengerId(), p.getDriverId(), p.getVehicleType(), p.getDistanceKm(), p.getDurationMinutes(),
                p.getBaseFare(), p.getDistanceCharge(), p.getTimeCharge(), p.getAmount(), p.getCurrency(),
                p.getPaymentMethod(), p.getCardLast4() == null ? null : "**** **** **** " + p.getCardLast4(),
                p.getStatus(), p.getPaidAt(), FareCalculator.RULE);
    }
}
