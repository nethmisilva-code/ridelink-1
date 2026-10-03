package com.ridelink.fare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentStatus;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    /** "First" keeps this safe even if older test data contains duplicate rideIds. */
    Optional<Payment> findFirstByRideIdOrderByCreatedAtDesc(String rideId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
}
