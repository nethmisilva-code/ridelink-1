package com.ridelink.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-chars";
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60);
        authService = new AuthService(accountRepository, passwordEncoder, jwtService, "admin@ridelink.lk",
                "Admin@12345", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void register_passenger_isActiveImmediately_andEmailIsNormalised() {
        when(accountRepository.existsByEmail("nadeesha@ridelink.test")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> withId(inv.getArgument(0), "acc-1"));

        AccountResponse response = authService.register(new RegisterRequest(" Nadeesha Perera ",
                " Nadeesha@RideLink.test ", "+94771234567", "Passw0rd!", Role.PASSENGER));

        assertThat(response.id()).isEqualTo("acc-1");
        assertThat(response.email()).isEqualTo("nadeesha@ridelink.test");
        assertThat(response.fullName()).isEqualTo("Nadeesha Perera");
        assertThat(response.role()).isEqualTo(Role.PASSENGER);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void register_driver_startsPending_andPasswordIsHashed() {
        when(accountRepository.existsByEmail("kasun@ridelink.test")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("bcrypt-hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> withId(inv.getArgument(0), "acc-2"));

        AccountResponse response = authService.register(new RegisterRequest("Kasun Silva", "kasun@ridelink.test",
                null, "Passw0rd!", Role.DRIVER));

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(response.status()).isEqualTo(AccountStatus.PENDING);
    }

    @Test
    void register_legacyCustomerRole_isStoredAsPassenger() {
        when(accountRepository.existsByEmail("legacy@ridelink.test")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> withId(inv.getArgument(0), "acc-3"));

        AccountResponse response = authService.register(new RegisterRequest("Legacy User", "legacy@ridelink.test",
                null, "Passw0rd!", Role.CUSTOMER));

        assertThat(response.role()).isEqualTo(Role.PASSENGER);
    }

    @Test
    void register_rejectsDuplicateEmail_withConflict() {
        when(accountRepository.existsByEmail("nadeesha@ridelink.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Nadeesha", "nadeesha@ridelink.test",
                null, "Passw0rd!", Role.PASSENGER)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void register_rejectsTheReservedAdminEmail() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("Fake Admin", "ADMIN@ridelink.lk",
                null, "Passw0rd!", Role.PASSENGER)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void login_succeeds_andTokenCarriesAccountIdAndRole() {
        Account account = account("acc-1", Role.PASSENGER, AccountStatus.ACTIVE);
        when(accountRepository.findByEmail("nadeesha@ridelink.test")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("nadeesha@ridelink.test", "Passw0rd!"));

        AuthenticatedUser user = jwtService.parse(response.token());
        assertThat(user.id()).isEqualTo("acc-1");
        assertThat(user.role()).isEqualTo("PASSENGER");
        assertThat(user.email()).isEqualTo("nadeesha@ridelink.test");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void login_legacyCustomerAccount_receivesPassengerToken() {
        when(accountRepository.findByEmail("old@ridelink.test"))
                .thenReturn(Optional.of(account("acc-9", Role.CUSTOMER, null)));
        when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("old@ridelink.test", "Passw0rd!"));

        assertThat(response.role()).isEqualTo("PASSENGER");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void login_wrongPassword_isUnauthorized() {
        when(accountRepository.findByEmail("nadeesha@ridelink.test"))
                .thenReturn(Optional.of(account("acc-1", Role.PASSENGER, AccountStatus.ACTIVE)));
        when(passwordEncoder.matches("wrong-pass", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadeesha@ridelink.test", "wrong-pass")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_unknownEmail_isUnauthorized() {
        when(accountRepository.findByEmail("nobody@ridelink.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@ridelink.test", "Passw0rd!")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_inactiveAccount_isForbidden() {
        when(accountRepository.findByEmail("nadeesha@ridelink.test"))
                .thenReturn(Optional.of(account("acc-1", Role.PASSENGER, AccountStatus.INACTIVE)));
        when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadeesha@ridelink.test", "Passw0rd!")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void login_adminCredentials_issueAdminToken_withoutDatabaseLookup() {
        LoginResponse response = authService.login(new LoginRequest("Admin@RideLink.lk", "Admin@12345"));

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(jwtService.parse(response.token()).isAdmin()).isTrue();
        verify(accountRepository, never()).findByEmail(any());
    }

    @Test
    void login_adminIsDisabled_whenNoAdminPasswordIsConfigured() {
        AuthService noAdmin = new AuthService(accountRepository, passwordEncoder, jwtService, "admin@ridelink.lk", "",
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(accountRepository.findByEmail("admin@ridelink.lk")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noAdmin.login(new LoginRequest("admin@ridelink.lk", "")))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static Account account(String id, Role role, AccountStatus status) {
        return new Account(id, "nadeesha@ridelink.test", "Nadeesha Perera", null, "hash", role, status, NOW, NOW);
    }

    private static Account withId(Account a, String id) {
        return new Account(id, a.email(), a.fullName(), a.phone(), a.passwordHash(), a.role(), a.status(),
                a.createdAt(), a.updatedAt());
    }
}
