package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.GlobalExceptionHandler;
import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.exception.NoDriverAvailableException;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.security.AuthenticatedUser;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer test (no Spring context, no database): request mapping, validation and error format. */
@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    private static final AuthenticatedUser PASSENGER = new AuthenticatedUser("rider-101", null, "PASSENGER");
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(PASSENGER, null, List.of());

    @Mock
    private RideService rideService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RideController(rideService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createRide_shouldReturnCreatedResponse() throws Exception {
        when(rideService.createRide(any(CreateRideRequest.class), eq(PASSENGER))).thenReturn(RideResponse.builder()
                .id("ride-123").riderId("rider-101").status(RideStatus.REQUESTED).estimatedFare(1326.0).build());

        mockMvc.perform(post("/api/v1/rides").principal(AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceArea":"Negombo","vehicleType":"SEDAN",
                                 "pickupLocation":{"address":"Negombo Beach Road","latitude":7.2087,"longitude":79.8358},
                                 "destinationLocation":{"address":"BIA","latitude":7.1808,"longitude":79.8842}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ride-123"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.driverId").doesNotExist());
    }

    @Test
    void createRide_withMissingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/rides").principal(AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickupLocation":{"address":"","latitude":95.0,"longitude":79.8}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.serviceArea").exists())
                .andExpect(jsonPath("$.fieldErrors.vehicleType").exists())
                .andExpect(jsonPath("$.fieldErrors.destinationLocation").exists())
                .andExpect(jsonPath("$.fieldErrors['pickupLocation.latitude']").exists());
        verifyNoInteractions(rideService);
    }

    @Test
    void assignDriver_withNoDriver_returns409() throws Exception {
        when(rideService.assignDriver(eq("ride-123"), isNull(), eq(PASSENGER)))
                .thenThrow(new NoDriverAvailableException("No available VAN driver in Jaffna right now."));

        mockMvc.perform(patch("/api/v1/rides/ride-123/assign-driver").principal(AUTH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No available VAN driver in Jaffna right now."));
    }

    @Test
    void invalidTransition_returns409InTheStandardErrorShape() throws Exception {
        when(rideService.startRide(eq("ride-123"), eq(PASSENGER)))
                .thenThrow(new InvalidStatusTransitionException(RideStatus.REQUESTED, RideStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/v1/rides/ride-123/start").principal(AUTH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/v1/rides/ride-123/start"));
    }
}
