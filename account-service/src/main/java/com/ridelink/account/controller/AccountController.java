package com.ridelink.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.security.AuthenticatedUser;
import com.ridelink.account.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "The signed-in account, and account lookup for other services")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "View my profile")
    public AccountResponse me(Authentication authentication) {
        return accountService.getMe(AuthenticatedUser.from(authentication));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update my profile (full name, phone)")
    public AccountResponse updateMe(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return accountService.updateProfile(AuthenticatedUser.from(authentication), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change my password")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Current password is incorrect")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        accountService.changePassword(AuthenticatedUser.from(authentication), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an account by id",
            description = "Allowed for the account owner, ADMIN and internal services. "
                    + "Called by Driver and Vehicle Service to verify driver accounts.")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "403", description = "Not your account")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public AccountResponse getById(@PathVariable String id, Authentication authentication) {
        return accountService.getById(id, AuthenticatedUser.from(authentication));
    }
}
