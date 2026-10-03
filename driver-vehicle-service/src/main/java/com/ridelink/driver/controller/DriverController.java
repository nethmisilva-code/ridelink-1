package com.ridelink.driver.controller;

import com.ridelink.driver.dto.AvailableDriverResponse;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateDriverRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.security.AuthenticatedUser;
import com.ridelink.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Drivers", description = "Driver operational profile, availability and location")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Create a driver operational profile",
               description = "Calls Account Service to verify the account is an approved (ACTIVE) DRIVER.")
    @ApiResponse(responseCode = "201", description = "Profile created")
    @ApiResponse(responseCode = "409", description = "Profile exists, wrong role, or account pending approval")
    @ApiResponse(responseCode = "503", description = "Account Service unavailable")
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request,
                                                 Authentication authentication,
                                                 @Parameter(hidden = true)
                                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DriverResponse.from(
                driverService.createProfile(request, AuthenticatedUser.from(authentication), authorization)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my driver profile")
    public DriverResponse me(Authentication authentication) {
        return DriverResponse.from(driverService.getMyProfile(AuthenticatedUser.from(authentication)));
    }

    @GetMapping("/available")
    @Operation(summary = "Retrieve eligible available drivers. Called by Ride Management Service.",
               description = "AVAILABLE drivers in the service area with an active vehicle of the requested type "
                       + "and a recently reported location.")
    public List<AvailableDriverResponse> available(
            @Parameter(example = "Negombo") @RequestParam String serviceArea,
            @Parameter(example = "SEDAN") @RequestParam VehicleType vehicleType) {
        return driverService.findEligibleDrivers(serviceArea, vehicleType);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a driver profile")
    public DriverResponse getOne(@PathVariable String id) {
        return DriverResponse.from(driverService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Update licence number or service area")
    public DriverResponse update(@PathVariable String id, @Valid @RequestBody UpdateDriverRequest request,
                                 Authentication authentication) {
        return DriverResponse.from(driverService.updateProfile(id, request, AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SERVICE')")
    @Operation(summary = "Change availability status (OFFLINE, AVAILABLE, ON_TRIP)")
    public DriverResponse setAvailability(@PathVariable String id,
                                          @Valid @RequestBody UpdateAvailabilityRequest request,
                                          Authentication authentication) {
        return DriverResponse.from(driverService.updateAvailability(id, request.availability(),
                AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Report simulated current location")
    public DriverResponse setLocation(@PathVariable String id,
                                      @Valid @RequestBody UpdateLocationRequest request,
                                      Authentication authentication) {
        return DriverResponse.from(driverService.updateLocation(id, request, AuthenticatedUser.from(authentication)));
    }
}
