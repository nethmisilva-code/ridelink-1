package com.ridelink.account.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.AuthenticatedUser;
import com.ridelink.account.security.JwtService;

@Service
public class AuthService {

    /** Subject used in the administrator's token (the admin is not stored in the database). */
    public static final String ADMIN_SUBJECT = "admin";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminUsername;
    private final String adminPassword;
    private final Clock clock;

    @Autowired
    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            @Value("${ridelink.admin.username:admin@ridelink.lk}") String adminUsername,
            @Value("${ridelink.admin.password:}") String adminPassword) {
        this(accountRepository, passwordEncoder, jwtService, adminUsername, adminPassword, Clock.systemUTC());
    }

    AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            String adminUsername, String adminPassword, Clock clock) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword == null ? "" : adminPassword;
        this.clock = clock;
    }

    /** Passengers are ACTIVE immediately; drivers are PENDING until an administrator approves them. */
    public AccountResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (email.equals(adminUsername) || accountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with email '" + email + "' already exists");
        }
        Role role = request.role().normalised();
        AccountStatus status = role == Role.DRIVER ? AccountStatus.PENDING : AccountStatus.ACTIVE;
        Instant now = clock.instant();
        Account saved = accountRepository.save(new Account(null, email, request.fullName().trim(),
                blankToNull(request.phone()), passwordEncoder.encode(request.password()), role, status, now, now));
        return AccountResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        String email = normaliseEmail(request.email());
        if (isAdminLogin(email, request.password())) {
            String token = jwtService.issue(ADMIN_SUBJECT, adminUsername, AuthenticatedUser.ADMIN);
            return new LoginResponse(token, "Bearer", jwtService.ttlSeconds(), ADMIN_SUBJECT, AuthenticatedUser.ADMIN,
                    AccountStatus.ACTIVE);
        }
        Account account = accountRepository.findByEmail(email).orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw invalidCredentials();
        }
        if (account.effectiveStatus() == AccountStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        String role = account.effectiveRole().name();
        String token = jwtService.issue(account.id(), account.email(), role);
        return new LoginResponse(token, "Bearer", jwtService.ttlSeconds(), account.id(), role,
                account.effectiveStatus());
    }

    private boolean isAdminLogin(String email, String password) {
        return !adminPassword.isEmpty() && email.equals(adminUsername) && password != null
                && MessageDigest.isEqual(adminPassword.getBytes(StandardCharsets.UTF_8),
                        password.getBytes(StandardCharsets.UTF_8));
    }

    private static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
