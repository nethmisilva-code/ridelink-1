package com.ridelink.fare.service;

import org.springframework.stereotype.Component;

import com.ridelink.fare.model.PaymentMethod;

/**
 * Stand-in for a payment gateway; no real money moves. Deterministic so the
 * failure scenario can be demonstrated on demand:
 * CASH and WALLET are always approved; CARD is approved unless the
 * (fictional) card number ends in 0002, which is always declined.
 */
@Component
public class PaymentSimulator {

    public static final String DECLINED_CARD_SUFFIX = "0002";

    public record Result(boolean approved, String failureReason) {
    }

    public Result charge(PaymentMethod method, String cardDigits) {
        if (method == PaymentMethod.CARD && cardDigits != null && cardDigits.endsWith(DECLINED_CARD_SUFFIX)) {
            return new Result(false, "Card declined by the simulated bank (test cards ending in 0002 are always "
                    + "declined). Try another card, CASH or WALLET.");
        }
        return new Result(true, null);
    }
}
