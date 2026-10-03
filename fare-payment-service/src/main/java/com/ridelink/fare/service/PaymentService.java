package com.ridelink.fare.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ridelink.fare.dto.FareBreakdown;
import com.ridelink.fare.dto.FinalFareRequest;
import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.exception.BadRequestException;
import com.ridelink.fare.exception.ConflictException;
import com.ridelink.fare.exception.ForbiddenException;
import com.ridelink.fare.exception.NotFoundException;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.security.AuthenticatedUser;

@Service
public class PaymentService {

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PaymentRepository paymentRepository;
    private final FareCalculator calculator;
    private final PaymentSimulator simulator;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, FareCalculator calculator, PaymentSimulator simulator) {
        this(paymentRepository, calculator, simulator, Clock.systemUTC());
    }

    PaymentService(PaymentRepository paymentRepository, FareCalculator calculator, PaymentSimulator simulator,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.calculator = calculator;
        this.simulator = simulator;
        this.clock = clock;
    }

    /** Outcome of creating the final fare; created is false when the same ride was already priced. */
    public record FinalFareResult(PaymentResponse payment, boolean created) {
    }

    /**
     * Called by Ride Management Service when a ride is completed. Idempotent:
     * repeating the call for the same ride returns the existing payment, so a
     * retry after a timeout can never create a second charge.
     */
    public FinalFareResult createFinalFare(FinalFareRequest request) {
        return paymentRepository.findFirstByRideIdOrderByCreatedAtDesc(request.rideId())
                .map(existing -> new FinalFareResult(PaymentResponse.from(existing), false))
                .orElseGet(() -> new FinalFareResult(PaymentResponse.from(paymentRepository.save(newPending(request))),
                        true));
    }

    private Payment newPending(FinalFareRequest request) {
        FareBreakdown fare = calculator.calculate(request.vehicleType(), request.distanceKm(), request.durationMinutes());
        Instant now = clock.instant();
        Payment payment = new Payment();
        payment.setRideId(request.rideId());
        payment.setPassengerId(request.passengerId());
        payment.setDriverId(request.driverId());
        payment.setDriverAccountId(request.driverAccountId());
        payment.setVehicleType(request.vehicleType());
        payment.setDistanceKm(fare.distanceKm());
        payment.setDurationMinutes(request.durationMinutes());
        payment.setBaseFare(fare.baseFare());
        payment.setDistanceCharge(fare.distanceCharge());
        payment.setTimeCharge(fare.timeCharge());
        payment.setAmount(fare.totalFare());
        payment.setCurrency(fare.currency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAttempts(0);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return payment;
    }

    /** Records a simulated payment. A FAILED payment may be retried; a SUCCESS payment cannot be paid twice. */
    public PaymentResponse pay(PaymentRequest request, AuthenticatedUser caller) {
        Payment payment = findByRide(request.rideId());
        if (!caller.isAdmin() && !(caller.isPassenger() && caller.id().equals(payment.getPassengerId()))) {
            throw new ForbiddenException("Only the passenger who took this ride can pay for it");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ConflictException("Ride " + request.rideId() + " has already been paid (receipt "
                    + payment.getReceiptNumber() + ")");
        }

        String cardDigits = request.cardNumber() == null ? null : request.cardNumber().replace(" ", "");
        if (request.paymentMethod() == PaymentMethod.CARD && (cardDigits == null || cardDigits.length() < 12
                || cardDigits.length() > 19)) {
            throw new BadRequestException("cardNumber (12 to 19 digits) is required for CARD payments");
        }

        PaymentSimulator.Result result = simulator.charge(request.paymentMethod(), cardDigits);
        Instant now = clock.instant();
        payment.setAttempts(payment.getAttempts() + 1);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setCardLast4(request.paymentMethod() == PaymentMethod.CARD
                ? cardDigits.substring(cardDigits.length() - 4) : null);
        payment.setUpdatedAt(now);
        if (result.approved()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("TXN-" + randomCode(10));
            payment.setReceiptNumber("RCPT-" + RECEIPT_DATE.format(now) + "-" + randomCode(6));
            payment.setFailureReason(null);
            payment.setPaidAt(now);
        }
        else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
        }
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public PaymentResponse getForRide(String rideId, AuthenticatedUser caller) {
        Payment payment = findByRide(rideId);
        assertCanView(payment, caller);
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getById(String paymentId, AuthenticatedUser caller) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        assertCanView(payment, caller);
        return PaymentResponse.from(payment);
    }

    public ReceiptResponse getReceipt(String rideId, AuthenticatedUser caller) {
        Payment payment = findByRide(rideId);
        assertCanView(payment, caller);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ConflictException("Ride " + rideId + " is not paid yet (payment status "
                    + payment.getStatus() + "), so there is no receipt");
        }
        return ReceiptResponse.from(payment);
    }

    public List<PaymentResponse> list(PaymentStatus status) {
        List<Payment> payments = status == null ? paymentRepository.findAllByOrderByCreatedAtDesc()
                : paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        return payments.stream().map(PaymentResponse::from).toList();
    }

    private Payment findByRide(String rideId) {
        return paymentRepository.findFirstByRideIdOrderByCreatedAtDesc(rideId)
                .orElseThrow(() -> new NotFoundException("No fare has been calculated for ride " + rideId
                        + " yet. The driver must complete the ride first."));
    }

    private static void assertCanView(Payment payment, AuthenticatedUser caller) {
        boolean passenger = caller.isPassenger() && caller.id().equals(payment.getPassengerId());
        boolean driver = caller.isDriver() && caller.id().equals(payment.getDriverAccountId());
        if (!caller.isPrivileged() && !passenger && !driver) {
            throw new ForbiddenException("You can only view payments for rides you took part in");
        }
    }

    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return code.toString().toUpperCase(Locale.ROOT);
    }
}
