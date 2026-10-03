package com.ridelink.driver.controller;

import com.ridelink.driver.exception.GlobalExceptionHandler;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer test (no Spring context, no database): binding, validation and error format. */
@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private DriverService driverService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DriverController(driverService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void available_returnsEmptyList_whenNoDriverMatches() throws Exception {
        when(driverService.findEligibleDrivers("Jaffna", VehicleType.VAN)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/drivers/available").param("serviceArea", "Jaffna").param("vehicleType", "VAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void available_rejectsUnknownVehicleType_with400() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/available").param("serviceArea", "Negombo").param("vehicleType", "BOAT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'BOAT' for parameter 'vehicleType'"));
        verifyNoInteractions(driverService);
    }

    @Test
    void location_withoutLatitude_isRejectedWith400() throws Exception {
        mockMvc.perform(patch("/api/v1/drivers/d1/location").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longitude\": 79.8358}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.latitude").value("latitude is required"));
        verifyNoInteractions(driverService);
    }

    @Test
    void unknownDriver_returns404InTheStandardErrorShape() throws Exception {
        when(driverService.getById("missing")).thenThrow(new ResourceNotFoundException("Driver not found: missing"));

        mockMvc.perform(get("/api/v1/drivers/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Driver not found: missing"))
                .andExpect(jsonPath("$.path").value("/api/v1/drivers/missing"));
    }
}
