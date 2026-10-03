package com.ridelink.ride.controller;

import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.security.AuthenticatedUser;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@Tag(name = "Rides", description = "Ride requests, driver assignment and the ride lifecycle")
public class RideController {

    private final RideService rideService;

    // ------------------------------------------------------------------
    // 1. Ride Request Creation
    // ------------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Request a ride (status REQUESTED)",
            description = "The estimated fare is calculated by Fare & Payment Service.")
    @ApiResponse(responseCode = "201", description = "Ride requested")
    @ApiResponse(responseCode = "409", description = "Passenger already has an active ride")
    @ApiResponse(responseCode = "503", description = "Fare & Payment Service unavailable")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request,
            Authentication authentication) {
        RideResponse response = rideService.createRide(request, AuthenticatedUser.from(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ------------------------------------------------------------------
    // 2. Driver Assignment
    // ------------------------------------------------------------------
    @PatchMapping("/{rideId}/assign-driver")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    @Operation(summary = "Assign the nearest eligible driver (status ASSIGNED)",
            description = "Calls Driver & Vehicle Service for eligible drivers, picks the nearest and reserves them.")
    @ApiResponse(responseCode = "200", description = "Driver assigned")
    @ApiResponse(responseCode = "409", description = "No available driver, or invalid status")
    public ResponseEntity<RideResponse> assignDriver(@PathVariable String rideId,
            @RequestBody(required = false) AssignDriverRequest request, Authentication authentication) {
        return ResponseEntity.ok(rideService.assignDriver(rideId, request, AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{rideId}/accept")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Assigned driver accepts the ride (status ACCEPTED)")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable String rideId, Authentication authentication) {
        return ResponseEntity.ok(rideService.acceptRide(rideId, AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{rideId}/decline")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Assigned driver declines (status back to REQUESTED, driver released)")
    public ResponseEntity<RideResponse> declineRide(@PathVariable String rideId, Authentication authentication) {
        return ResponseEntity.ok(rideService.declineRide(rideId, AuthenticatedUser.from(authentication)));
    }

    // ------------------------------------------------------------------
    // 3. Ride Status Lifecycle Management
    // ------------------------------------------------------------------
    @PatchMapping("/{rideId}/start")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Start an accepted ride (status IN_PROGRESS)")
    public ResponseEntity<RideResponse> startRide(@PathVariable String rideId, Authentication authentication) {
        return ResponseEntity.ok(rideService.startRide(rideId, AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{rideId}/complete")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Complete an in-progress ride (status COMPLETED)",
            description = "Fare & Payment Service calculates the final fare and opens a PENDING payment.")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String rideId,
            @Valid @RequestBody(required = false) CompleteRideRequest request, Authentication authentication) {
        return ResponseEntity.ok(rideService.completeRide(rideId, request, AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{rideId}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER','DRIVER','ADMIN')")
    @Operation(summary = "Cancel a ride before it starts (status CANCELLED)")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable String rideId,
            @Valid @RequestBody(required = false) CancelRideRequest request, Authentication authentication) {
        return ResponseEntity.ok(rideService.cancelRide(rideId, request, AuthenticatedUser.from(authentication)));
    }

    // ------------------------------------------------------------------
    // 4. Ride Retrieval
    // ------------------------------------------------------------------
    @GetMapping("/me")
    @Operation(summary = "My rides (as passenger or as driver), newest first")
    public ResponseEntity<List<RideResponse>> getMyRides(Authentication authentication) {
        return ResponseEntity.ok(rideService.getMyRides(AuthenticatedUser.from(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All rides (ADMIN)")
    public ResponseEntity<List<RideResponse>> getAllRides() {
        return ResponseEntity.ok(rideService.getAllRides());
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Active rides: REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS (ADMIN)")
    public ResponseEntity<List<RideResponse>> getActiveRides() {
        return ResponseEntity.ok(rideService.getActiveRides());
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get a ride (passenger, assigned driver or ADMIN)")
    public ResponseEntity<RideResponse> getRideById(@PathVariable String rideId, Authentication authentication) {
        return ResponseEntity.ok(rideService.getRideById(rideId, AuthenticatedUser.from(authentication)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    @Operation(summary = "Rides of a passenger (the passenger themself or ADMIN)")
    public ResponseEntity<List<RideResponse>> getRidesByUser(@PathVariable String userId,
            Authentication authentication) {
        return ResponseEntity.ok(rideService.getRidesByUserId(userId, AuthenticatedUser.from(authentication)));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rides of a driver profile (ADMIN)")
    public ResponseEntity<List<RideResponse>> getRidesByDriver(@PathVariable String driverId) {
        return ResponseEntity.ok(rideService.getRidesByDriverId(driverId));
    }
}
