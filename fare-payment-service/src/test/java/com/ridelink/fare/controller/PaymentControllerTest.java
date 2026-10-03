package com.ridelink.fare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.exception.GlobalExceptionHandler;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.security.AuthenticatedUser;
import com.ridelink.fare.service.FareCalculator;
import com.ridelink.fare.service.FareService;
import com.ridelink.fare.service.PaymentService;

/** Web-layer test (no Spring context, no database): status codes, validation and error format. */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final AuthenticatedUser PASSENGER = new AuthenticatedUser("rider-101", null, "PASSENGER");
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(PASSENGER, null, List.of());

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService),
                        new FareController(new FareService(new FareCalculator()), paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void declinedPayment_returns402_withTheFailedPaymentInTheBody() throws Exception {
        when(paymentService.pay(any(PaymentRequest.class), eq(PASSENGER))).thenReturn(payment(PaymentStatus.FAILED));

        mockMvc.perform(post("/api/v1/payments").principal(AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"ride-123\",\"paymentMethod\":\"CARD\",\"cardNumber\":\"4000000000000002\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void successfulPayment_returns200() throws Exception {
        when(paymentService.pay(any(PaymentRequest.class), eq(PASSENGER))).thenReturn(payment(PaymentStatus.SUCCESS));

        mockMvc.perform(post("/api/v1/payments").principal(AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"ride-123\",\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void payment_withUnknownMethod_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments").principal(AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"ride-123\",\"paymentMethod\":\"CHEQUE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService);
    }

    @Test
    void estimate_withoutVehicleType_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/fares/estimate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distanceKm\": 8.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.vehicleType").exists());
    }

    @Test
    void rates_listTheRule() throws Exception {
        mockMvc.perform(get("/api/v1/fares/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rates.length()").value(4));
    }

    private static PaymentResponse payment(PaymentStatus status) {
        return new PaymentResponse("pay-1", "ride-123", "rider-101", "driver-55", "acc-driver-55", null, null, null,
                null, null, null, new BigDecimal("1460.00"), "LKR", status, PaymentMethod.CARD, "0002", null, null,
                null, 1, null, null);
    }
}
