package com.ridelink.driver.repository;

import com.ridelink.driver.model.AvailabilityStatus;
import com.ridelink.driver.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends MongoRepository<Driver, String>, DriverRepositoryCustom {
    Optional<Driver> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
    List<Driver> findByAvailabilityAndServiceAreaIgnoreCase(AvailabilityStatus availability, String serviceArea);
}