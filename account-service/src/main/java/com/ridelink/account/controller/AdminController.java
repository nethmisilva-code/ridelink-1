package com.ridelink.account.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.UpdateAccountStatusRequest;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Every path under /api/v1/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@Tag(name = "Admin", description = "Administrator operations (ADMIN role)")
public class AdminController {

    private final AccountService accountService;

    public AdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "List accounts, optionally filtered by role and status")
    public List<AccountResponse> list(@RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status) {
        return accountService.listAccounts(role, status);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Approve, suspend or reactivate an account",
            description = "Set a PENDING driver to ACTIVE to allow a driver profile to be created.")
    public AccountResponse setStatus(@PathVariable String id, @Valid @RequestBody UpdateAccountStatusRequest request) {
        return accountService.setStatus(id, request.status());
    }
}
