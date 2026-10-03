package com.ridelink.driver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "drivers")
public class Driver {

    @Id
    private String id;

    @Indexed(unique = true)
    private String accountId;          // owned by Account Service, stored as a stable identifier only

    private String licenseNumber;

    @Indexed
    private String serviceArea;

    private AvailabilityStatus availability = AvailabilityStatus.OFFLINE;
    private Location currentLocation;
    private Instant locationUpdatedAt;  // used to ignore drivers with stale (simulated) GPS positions
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }

    public AvailabilityStatus getAvailability() { return availability; }
    public void setAvailability(AvailabilityStatus availability) { this.availability = availability; }

    public Location getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Location currentLocation) { this.currentLocation = currentLocation; }

    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(Instant locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
