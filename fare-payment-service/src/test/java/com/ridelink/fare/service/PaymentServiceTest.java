package com.ridelink.fare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.ridelink.fare.model.VehicleType;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:30:00Z");
    private static final AuthenticatedUser PASSENGER = new AuthenticatedUser("rider-101", null, "PASSENGER");
    private static final AuthenticatedUser OTHER_PASSENGER = new AuthenticatedUser("rider-999", null, "PASSENGER");
    private static final AuthenticatedUser DRIVER = new AuthenticatedUser("acc-driver-55", null, "DRIVER");
    private static final AuthenticatedUser RIDE_SERVICE = new AuthenticatedUser("ride-management-service", null, "SERVICE");

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, new FareCalculator(), new PaymentSimulator(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createFinalFare_opensAPendingPayment_withTheFareBreakdown() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-1");
            return p;
        });

        PaymentService.FinalFareResult result = paymentService.createFinalFare(finalFareRequest());

        assertThat(result.created()).isTrue();
        PaymentResponse payment = result.payment();
        assertThat(payment.id()).isEqualTo("pay-1");
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
        // SEDAN: 200 + 120 * 9.5 + 8 * 15 = 1460.00
        assertThat(payment.amount()).isEqualByComparingTo("1460.00");
        assertThat(payment.distanceCharge()).isEqualByComparingTo("1140.00");
        assertThat(payment.timeCharge()).isEqualByComparingTo("120.00");
    }

    @Test
    void createFinalFare_isIdempotent_forTheSameRide() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123"))
                .thenReturn(Optional.of(pending()));

        PaymentService.FinalFareResult result = paymentService.createFinalFare(finalFareRequest());

        assertThat(result.created()).isFalse();
        assertThat(result.payment().id()).isEqualTo("pay-1");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pay_withCash_succeeds_andIssuesTransactionAndReceiptNumbers() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(pending()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse paid = paymentService.pay(new PaymentRequest("ride-123", PaymentMethod.CASH, null), PASSENGER);

        assertThat(paid.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paid.transactionId()).startsWith("TXN-").hasSize(14);
        assertThat(paid.receiptNumber()).startsWith("RCPT-20260925-");
        assertThat(paid.paidAt()).isEqualTo(NOW);
        assertThat(paid.attempts()).isEqualTo(1);
    }

    @Test
    void pay_withTheDeclinedTestCard_fails_andStoresOnlyTheLastFourDigits() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(pending()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse failed = paymentService.pay(
                new PaymentRequest("ride-123", PaymentMethod.CARD, "4000 0000 0000 0002"), PASSENGER);

        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.failureReason()).contains("declined");
        assertThat(failed.cardLast4()).isEqualTo("0002");
        assertThat(failed.receiptNumber()).isNull();
    }

    @Test
    void pay_afterAFailedAttempt_canBeRetried() {
        Payment payment = pending();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setAttempts(1);
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse paid = paymentService.pay(
                new PaymentRequest("ride-123", PaymentMethod.CARD, "4242424242424242"), PASSENGER);

        assertThat(paid.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paid.attempts()).isEqualTo(2);
        assertThat(paid.failureReason()).isNull();
    }

    @Test
    void pay_twice_isAConflict() {
        Payment payment = pending();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setReceiptNumber("RCPT-20260925-ABC123");
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest("ride-123", PaymentMethod.CASH, null), PASSENGER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been paid");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pay_byAnotherPassenger_isForbidden() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest("ride-123", PaymentMethod.CASH, null),
                OTHER_PASSENGER)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void pay_byCard_withoutACardNumber_isABadRequest() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest("ride-123", PaymentMethod.CARD, null), PASSENGER))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void pay_forARideThatWasNeverCompleted_isNotFound() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest("ride-x", PaymentMethod.CASH, null), PASSENGER))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("complete the ride first");
    }

    @Test
    void receipt_beforePayment_isAConflict() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> paymentService.getReceipt("ride-123", PASSENGER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not paid yet");
    }

    @Test
    void receipt_isVisibleToTheDriverOfTheRide_withAMaskedCard() {
        Payment payment = pending();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setCardLast4("4242");
        payment.setReceiptNumber("RCPT-20260925-ABC123");
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(payment));

        ReceiptResponse receipt = paymentService.getReceipt("ride-123", DRIVER);

        assertThat(receipt.receiptNumber()).isEqualTo("RCPT-20260925-ABC123");
        assertThat(receipt.maskedCard()).isEqualTo("**** **** **** 4242");
        assertThat(receipt.totalAmount()).isEqualByComparingTo("1460.00");
    }

    @Test
    void paymentStatus_isHiddenFromUnrelatedUsers_butVisibleToTheRideService() {
        when(paymentRepository.findFirstByRideIdOrderByCreatedAtDesc("ride-123")).thenReturn(Optional.of(pending()));

        assertThat(paymentService.getForRide("ride-123", RIDE_SERVICE).status()).isEqualTo(PaymentStatus.PENDING);
        assertThatThrownBy(() -> paymentService.getForRide("ride-123", OTHER_PASSENGER))
                .isInstanceOf(ForbiddenException.class);
    }

    private static FinalFareRequest finalFareRequest() {
        return new FinalFareRequest("ride-123", "rider-101", "driver-55", "acc-driver-55", VehicleType.SEDAN,
                new BigDecimal("9.5"), 15);
    }

    private static Payment pending() {
        Payment p = new Payment();
        p.setId("pay-1");
        p.setRideId("ride-123");
        p.setPassengerId("rider-101");
        p.setDriverId("driver-55");
        p.setDriverAccountId("acc-driver-55");
        p.setVehicleType(VehicleType.SEDAN);
        p.setDistanceKm(new BigDecimal("9.50"));
        p.setDurationMinutes(15);
        p.setBaseFare(new BigDecimal("200.00"));
        p.setDistanceCharge(new BigDecimal("1140.00"));
        p.setTimeCharge(new BigDecimal("120.00"));
        p.setAmount(new BigDecimal("1460.00"));
        p.setCurrency("LKR");
        p.setStatus(PaymentStatus.PENDING);
        p.setCreatedAt(NOW.minusSeconds(60));
        return p;
    }
}
