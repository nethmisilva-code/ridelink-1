package com.ridelink.fare.model;

import java.math.BigDecimal;

/**
 * Rate card in LKR (shared vehicle vocabulary with the other services).
 * TUK and SEDAN keep the base and per-km rates of the original service
 * (TUK 100 + 80/km, CAR 200 + 120/km); VAN and SUV were added for the
 * vehicle types offered by Driver &amp; Vehicle Service.
 */
public enum VehicleType {
    TUK("100", "80", "5"),
    SEDAN("200", "120", "8"),
    VAN("300", "150", "10"),
    SUV("350", "170", "12");

    private final BigDecimal baseFare;
    private final BigDecimal perKm;
    private final BigDecimal perMinute;

    VehicleType(String baseFare, String perKm, String perMinute) {
        this.baseFare = new BigDecimal(baseFare);
        this.perKm = new BigDecimal(perKm);
        this.perMinute = new BigDecimal(perMinute);
    }

    public BigDecimal baseFare() {
        return baseFare;
    }

    public BigDecimal perKm() {
        return perKm;
    }

    public BigDecimal perMinute() {
        return perMinute;
    }
}
