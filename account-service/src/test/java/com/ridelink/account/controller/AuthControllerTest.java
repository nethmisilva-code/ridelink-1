package com.ridelink.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.exception.GlobalExceptionHandler;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.service.AuthService;

/** Web-layer test (no Spring context, no database): request mapping, validation and error format. */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AccountResponse("acc-1",
                "Nadeesha Perera", "nadeesha@ridelink.test", null, Role.PASSENGER, AccountStatus.ACTIVE,
                Instant.parse("2026-09-25T10:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Nadeesha Perera","email":"nadeesha@ridelink.test",
                         "password":"Passw0rd!","role":"PASSENGER"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("acc-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void register_invalidRequest_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"N","email":"not-an-email","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }
}
