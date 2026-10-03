package com.ridelink.fare.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * One payment record per completed ride, owned by Fare &amp; Payment Service.
 * Ride, passenger and driver are referenced by stable identifiers only.
 * Money is BigDecimal stored as Decimal128 so amounts are exact.
 * Only the last four card digits are ever stored.
 */
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed
    private String rideId;

    @Indexed
    private String passengerId;

    private String driverId;
    private String driverAccountId;
    private VehicleType vehicleType;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal distanceKm;

    private Integer durationMinutes;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal baseFare;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal distanceCharge;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal timeCharge;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private String currency;

    @Field("paymentStatus") // field name kept from the original service
    private PaymentStatus status;

    private PaymentMethod paymentMethod;
    private String cardLast4;
    private String transactionId;
    private String receiptNumber;
    private String failureReason;
    private int attempts;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;

    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getDriverAccountId() { return driverAccountId; }
    public void setDriverAccountId(String driverAccountId) { this.driverAccountId = driverAccountId; }
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getDistanceCharge() { return distanceCharge; }
    public void setDistanceCharge(BigDecimal distanceCharge) { this.distanceCharge = distanceCharge; }
    public BigDecimal getTimeCharge() { return timeCharge; }
    public void setTimeCharge(BigDecimal timeCharge) { this.timeCharge = timeCharge; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
