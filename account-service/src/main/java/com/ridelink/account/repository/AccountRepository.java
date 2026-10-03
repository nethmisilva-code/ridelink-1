package com.ridelink.account.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ridelink.account.model.Account;

public interface AccountRepository extends MongoRepository<Account, String> {

    boolean existsByEmail(String email);

    Optional<Account> findByEmail(String email);
}
