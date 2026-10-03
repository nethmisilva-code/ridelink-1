package com.ridelink.account.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.AuthenticatedUser;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this(accountRepository, passwordEncoder, Clock.systemUTC());
    }

    AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public AccountResponse getMe(AuthenticatedUser caller) {
        return AccountResponse.from(findOwnAccount(caller));
    }

    public AccountResponse updateProfile(AuthenticatedUser caller, UpdateProfileRequest request) {
        Account account = findOwnAccount(caller);
        String fullName = request.fullName() != null ? request.fullName().trim() : account.fullName();
        String phone = request.phone() != null ? (request.phone().isBlank() ? null : request.phone().trim())
                : account.phone();
        return AccountResponse.from(accountRepository.save(account.withProfile(fullName, phone, clock.instant())));
    }

    public void changePassword(AuthenticatedUser caller, ChangePasswordRequest request) {
        Account account = findOwnAccount(caller);
        if (!passwordEncoder.matches(request.currentPassword(), account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        accountRepository.save(account.withPasswordHash(passwordEncoder.encode(request.newPassword()), clock.instant()));
    }

    /**
     * Account lookup used by other services (Driver and Vehicle Service calls this
     * to verify a driver account). Allowed for the account owner, ADMIN and SERVICE.
     */
    public AccountResponse getById(String id, AuthenticatedUser caller) {
        if (!caller.isPrivileged() && !caller.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own account");
        }
        return AccountResponse.from(findById(id));
    }

    public List<AccountResponse> listAccounts(Role role, AccountStatus status) {
        Role wanted = role == null ? null : role.normalised();
        return accountRepository.findAll().stream()
                .filter(a -> wanted == null || a.effectiveRole() == wanted)
                .filter(a -> status == null || a.effectiveStatus() == status)
                .sorted(Comparator.comparing(Account::email, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse setStatus(String id, AccountStatus status) {
        Account account = findById(id);
        Instant now = clock.instant();
        return AccountResponse.from(accountRepository.save(account.withStatus(status, now)));
    }

    private Account findOwnAccount(AuthenticatedUser caller) {
        if (caller.isAdmin() && AuthService.ADMIN_SUBJECT.equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The administrator is configured through environment variables and has no stored profile");
        }
        return findById(caller.id());
    }

    private Account findById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id));
    }
}
