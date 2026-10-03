package com.ridelink.driver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "vehicles")
public class Vehicle {

    @Id
    private String id;

    @Indexed
    private String driverId;              // stable identifier of the owning Driver document

    @Indexed(unique = true)
    private String plateNumber;

    private String make;

    @Field("modelName")                   // avoids clashing with Mongo's reserved-ish naming and keeps intent clear
    private String model;

    private int year;
    private String colour;
    private VehicleType vehicleType;
    private int seats;
    private boolean active = true;        // soft delete, so ride history never points at a missing vehicle

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    /** Required by Spring Data MongoDB to instantiate documents during mapping. */
public Vehicle() {
    // Intentionally empty. Field values are populated by the mapping layer.
}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}