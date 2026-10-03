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
import java.util.List;
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
import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final AuthenticatedUser PASSENGER = new AuthenticatedUser("acc-1", "p@ridelink.test", "PASSENGER");
    private static final AuthenticatedUser OTHER = new AuthenticatedUser("acc-2", "o@ridelink.test", "PASSENGER");
    private static final AuthenticatedUser SERVICE = new AuthenticatedUser("driver-vehicle-service", null, "SERVICE");
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(AuthService.ADMIN_SUBJECT, null, "ADMIN");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getMe_returnsTheCallersAccount() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account("acc-1", Role.PASSENGER)));

        assertThat(accountService.getMe(PASSENGER).email()).isEqualTo("acc-1@ridelink.test");
    }

    @Test
    void getMe_forBuiltInAdmin_isNotFound() {
        assertThatThrownBy(() -> accountService.getMe(ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator");
    }

    @Test
    void updateProfile_changesOnlyTheFieldsProvided() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account("acc-1", Role.PASSENGER)));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse updated = accountService.updateProfile(PASSENGER, new UpdateProfileRequest(null, "+94770000000"));

        assertThat(updated.fullName()).isEqualTo("User acc-1");
        assertThat(updated.phone()).isEqualTo("+94770000000");
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account("acc-1", Role.PASSENGER)));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> accountService.changePassword(PASSENGER, new ChangePasswordRequest("bad", "NewPassw0rd")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current password is incorrect");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void changePassword_storesTheNewHash() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account("acc-1", Role.PASSENGER)));
        when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("new-hash");

        accountService.changePassword(PASSENGER, new ChangePasswordRequest("Passw0rd!", "NewPassw0rd"));

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo("new-hash");
    }

    @Test
    void getById_allowsOwnerServiceAndAdmin() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account("acc-1", Role.DRIVER)));

        assertThat(accountService.getById("acc-1", PASSENGER).id()).isEqualTo("acc-1");
        assertThat(accountService.getById("acc-1", SERVICE).role()).isEqualTo(Role.DRIVER);
        assertThat(accountService.getById("acc-1", ADMIN).id()).isEqualTo("acc-1");
    }

    @Test
    void getById_rejectsAnotherUsersAccount_withForbidden() {
        assertThatThrownBy(() -> accountService.getById("acc-1", OTHER))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getById_unknownId_isNotFound() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getById("missing", ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void setStatus_approvesPendingDriver() {
        Account pending = new Account("acc-7", "k@ridelink.test", "Kasun", null, "hash", Role.DRIVER,
                AccountStatus.PENDING, NOW, NOW);
        when(accountRepository.findById("acc-7")).thenReturn(Optional.of(pending));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(accountService.setStatus("acc-7", AccountStatus.ACTIVE).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void listAccounts_filtersByRole_includingLegacyCustomerAsPassenger() {
        when(accountRepository.findAll()).thenReturn(List.of(account("a", Role.PASSENGER), account("b", Role.CUSTOMER),
                account("c", Role.DRIVER)));

        List<AccountResponse> passengers = accountService.listAccounts(Role.PASSENGER, null);

        assertThat(passengers).extracting(AccountResponse::id).containsExactly("a", "b");
    }

    private static Account account(String id, Role role) {
        return new Account(id, id + "@ridelink.test", "User " + id, null, "hash", role, AccountStatus.ACTIVE, NOW, NOW);
    }
}
