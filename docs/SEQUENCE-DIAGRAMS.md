# Sequence diagrams

## 1. Driver onboarding (Driver & Vehicle calls Account)

```mermaid
sequenceDiagram
    autonumber
    actor D as Driver
    actor A as Admin
    participant ACC as Account :8081
    participant DRV as Driver & Vehicle :8082

    D->>ACC: POST /auth/register {role: DRIVER}
    ACC-->>D: 201 {id, status: PENDING}
    A->>ACC: POST /auth/login (admin)
    A->>ACC: PATCH /admin/accounts/{id}/status {ACTIVE}
    ACC-->>A: 200 {status: ACTIVE}
    D->>ACC: POST /auth/login
    ACC-->>D: 200 {token (sub = account id, role = DRIVER)}
    D->>DRV: POST /drivers {licenseNumber, serviceArea} + Bearer token
    DRV->>ACC: GET /accounts/{id} (forwards driver token)
    ACC-->>DRV: 200 {role: DRIVER, status: ACTIVE}
    DRV-->>D: 201 {driverId, availability: OFFLINE}
    D->>DRV: POST /vehicles, PATCH /drivers/{id}/location
    D->>DRV: PATCH /drivers/{id}/availability {AVAILABLE}
    DRV-->>D: 200 (requires an active vehicle and a location)
```

If the account is still PENDING, step 10 returns 409. If Account Service is down, the driver gets 503 and no profile is created.

## 2. Ride request and assignment (Ride calls Fare, then Driver)

```mermaid
sequenceDiagram
    autonumber
    actor P as Passenger
    participant RID as Ride Management :8083
    participant FAR as Fare & Payment :8084
    participant DRV as Driver & Vehicle :8082

    P->>RID: POST /rides {pickup, destination, serviceArea, vehicleType}
    RID->>FAR: POST /fares/estimate (SERVICE token)
    FAR-->>RID: 200 {distanceKm, estimatedDurationMinutes, totalFare}
    RID-->>P: 201 {status: REQUESTED, estimatedFare}
    P->>RID: PATCH /rides/{id}/assign-driver
    RID->>DRV: GET /drivers/available?serviceArea&vehicleType (SERVICE token)
    DRV-->>RID: 200 [AVAILABLE drivers with an active vehicle and a fresh location]
    Note over RID: sort by distance to pickup, then freshest location
    RID->>DRV: PATCH /drivers/{nearest}/availability {ON_TRIP}
    alt reserved
        DRV-->>RID: 200
        RID-->>P: 200 {status: ASSIGNED, driverId, vehicleNumber}
    else driver was taken (409)
        DRV-->>RID: 409
        Note over RID: try the next nearest driver
    end
```

With an empty list, step 7 ends with 409 "No available driver" and the ride stays REQUESTED, so the passenger can retry or cancel.

## 3. Lifecycle, completion and payment (Ride calls Fare and Driver)

```mermaid
sequenceDiagram
    autonumber
    actor D as Driver
    actor P as Passenger
    participant RID as Ride Management :8083
    participant FAR as Fare & Payment :8084
    participant DRV as Driver & Vehicle :8082

    D->>RID: PATCH /rides/{id}/accept
    RID-->>D: 200 {ACCEPTED}
    D->>RID: PATCH /rides/{id}/start
    RID-->>D: 200 {IN_PROGRESS}
    D->>RID: PATCH /rides/{id}/complete {actualDistanceKm}
    Note over RID: duration = startedAt to now (server clock)
    RID->>FAR: POST /fares/final {rideId, passengerId, driverId, vehicleType, distanceKm, durationMinutes}
    FAR-->>RID: 200 {paymentId, amount, status: PENDING} (idempotent per ride)
    RID->>DRV: PATCH /drivers/{id}/availability {AVAILABLE} (best effort)
    RID-->>D: 200 {COMPLETED, finalFare, paymentId}
    P->>FAR: POST /payments {rideId, CARD, ...0002}
    FAR-->>P: 402 {status: FAILED, failureReason}
    P->>FAR: POST /payments {rideId, CASH}
    FAR-->>P: 200 {status: SUCCESS, transactionId, receiptNumber}
    P->>FAR: GET /payments/ride/{rideId}/receipt
    FAR-->>P: 200 receipt
```

If Fare & Payment is down at step 5, the driver gets 503 and the ride stays IN_PROGRESS, so no ride is ever completed without a fare. Because `/fares/final` is idempotent, retrying never creates a second charge.

## Ride state machine

```mermaid
stateDiagram-v2
    [*] --> REQUESTED: passenger creates ride
    REQUESTED --> ASSIGNED: nearest driver reserved
    REQUESTED --> CANCELLED
    ASSIGNED --> ACCEPTED: driver accepts
    ASSIGNED --> REQUESTED: driver declines (driver released)
    ASSIGNED --> CANCELLED: driver released
    ACCEPTED --> IN_PROGRESS: driver starts
    ACCEPTED --> CANCELLED: driver released
    IN_PROGRESS --> COMPLETED: final fare calculated, driver released
    COMPLETED --> [*]
    CANCELLED --> [*]
```

Any other transition returns 409 with the current and requested status. An in-progress trip cannot be cancelled: it must be completed so it can be charged.
