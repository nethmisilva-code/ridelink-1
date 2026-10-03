package com.ridelink.driver.repository;

import java.time.Instant;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.ridelink.driver.model.AvailabilityStatus;
import com.ridelink.driver.model.Driver;

/**
 * Compare-and-set on availability. MongoDB applies a single-document update
 * atomically, so when two rides try to reserve the same driver at the same
 * moment, exactly one update matches "availability = AVAILABLE" and wins.
 * Spring Data picks this class up by its name (fragment interface + "Impl").
 */
public class DriverRepositoryCustomImpl implements DriverRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public DriverRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean reserveIfAvailable(String driverId, Instant now) {
        Query query = Query.query(Criteria.where("id").is(driverId)
                .and("availability").is(AvailabilityStatus.AVAILABLE));
        Update update = new Update().set("availability", AvailabilityStatus.ON_TRIP).set("updatedAt", now);
        return mongoTemplate.updateFirst(query, update, Driver.class).getModifiedCount() == 1;
    }
}
