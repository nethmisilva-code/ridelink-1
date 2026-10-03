package com.ridelink.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration and login (public)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a passenger or driver account",
            description = "Passengers become ACTIVE immediately. Drivers start PENDING until an admin approves them.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT", description = "Use the token as 'Authorization: Bearer <token>' "
            + "on every other RideLink service.")
    @ApiResponse(responseCode = "200", description = "Logged in")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    @ApiResponse(responseCode = "403", description = "Account deactivated")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
