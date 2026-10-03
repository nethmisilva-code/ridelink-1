package com.ridelink.driver.controller;

import com.ridelink.driver.dto.CreateVehicleRequest;
import com.ridelink.driver.dto.UpdateVehicleActiveRequest;
import com.ridelink.driver.dto.UpdateVehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.security.AuthenticatedUser;
import com.ridelink.driver.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Vehicle registration and operational details")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Register a vehicle for a driver")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VehicleResponse.from(vehicleService.createVehicle(request, AuthenticatedUser.from(authentication))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a vehicle")
    public VehicleResponse getOne(@PathVariable String id) {
        return VehicleResponse.from(vehicleService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List a driver's vehicles")
    public List<VehicleResponse> listByDriver(@RequestParam String driverId) {
        return vehicleService.listByDriver(driverId).stream().map(VehicleResponse::from).toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Update editable vehicle details (make, model, year, colour, seats)")
    public VehicleResponse update(@PathVariable String id, @Valid @RequestBody UpdateVehicleRequest request,
                                  Authentication authentication) {
        return VehicleResponse.from(vehicleService.updateDetails(id, request, AuthenticatedUser.from(authentication)));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary = "Activate or deactivate a vehicle")
    public VehicleResponse setActive(@PathVariable String id, @Valid @RequestBody UpdateVehicleActiveRequest request,
                                     Authentication authentication) {
        return VehicleResponse.from(vehicleService.setActive(id, request.active(), AuthenticatedUser.from(authentication)));
    }
}
