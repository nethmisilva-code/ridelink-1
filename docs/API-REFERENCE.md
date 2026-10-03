# API reference

All paths start with `/api/v1`. Every endpoint except register, login, the rate card, Swagger and health needs `Authorization: Bearer <token>`. Role rules are enforced by Spring Security; ownership rules ("only your own ride") are enforced in the service layer.

## Account Service (8081)

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Register a PASSENGER (ACTIVE) or DRIVER (PENDING) |
| POST | `/auth/login` | public | Returns `token`, `accountId`, `role`, `status` |
| GET | `/accounts/me` | any user | My profile |
| PATCH | `/accounts/me` | any user | Update full name or phone |
| PUT | `/accounts/me/password` | any user | Change password (204) |
| GET | `/accounts/{id}` | owner, ADMIN, SERVICE | Account lookup (used by Driver & Vehicle) |
| GET | `/admin/accounts?role=&status=` | ADMIN | List accounts |
| PATCH | `/admin/accounts/{id}/status` | ADMIN | `{ "status": "ACTIVE" }` approves a driver, `INACTIVE` blocks login |

## Driver & Vehicle Service (8082)

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/drivers` | DRIVER (own account), ADMIN | Create profile after checking the account with Account Service |
| GET | `/drivers/me` | DRIVER | My driver profile |
| GET | `/drivers/{id}` | any user | Driver profile |
| PATCH | `/drivers/{id}` | owner, ADMIN | Update licence number or service area |
| PATCH | `/drivers/{id}/location` | owner, ADMIN | `{ latitude, longitude }` simulated location |
| PATCH | `/drivers/{id}/availability` | owner, ADMIN, SERVICE | `OFFLINE`, `AVAILABLE` (needs vehicle and location), `ON_TRIP` (SERVICE or ADMIN only) |
| GET | `/drivers/available?serviceArea=&vehicleType=` | PASSENGER, DRIVER, ADMIN, SERVICE | Eligible drivers (used by Ride Management) |
| POST | `/vehicles` | owner, ADMIN | Register a vehicle (plate must be unique) |
| GET | `/vehicles/{id}`, `/vehicles?driverId=` | any user | Vehicle details |
| PATCH | `/vehicles/{id}` | owner, ADMIN | Update make, model, year, colour, seats |
| PATCH | `/vehicles/{id}/active` | owner, ADMIN | Activate or deactivate (not the last active vehicle) |

**Eligibility rule:** AVAILABLE, same service area (case-insensitive), an active vehicle of the requested type, and a location reported within the last 30 minutes.

## Ride Management Service (8083)

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/rides` | PASSENGER | `{ pickupLocation, destinationLocation, serviceArea, vehicleType }`; fare estimated by Fare & Payment; status REQUESTED |
| PATCH | `/rides/{id}/assign-driver` | owner PASSENGER, ADMIN | Nearest eligible driver; ADMIN may send `{ "driverId": ... }`; status ASSIGNED |
| PATCH | `/rides/{id}/accept` | assigned DRIVER | ACCEPTED |
| PATCH | `/rides/{id}/decline` | assigned DRIVER | Back to REQUESTED, driver released |
| PATCH | `/rides/{id}/start` | assigned DRIVER | IN_PROGRESS |
| PATCH | `/rides/{id}/complete` | assigned DRIVER | `{ "actualDistanceKm": 9.5 }` optional; final fare from Fare & Payment; COMPLETED |
| PATCH | `/rides/{id}/cancel` | owner PASSENGER, assigned DRIVER, ADMIN | `{ "reason": ... }`; not allowed once IN_PROGRESS |
| GET | `/rides/{id}` | participants, ADMIN, SERVICE | Ride details |
| GET | `/rides/me` | PASSENGER or DRIVER | My rides, newest first |
| GET | `/rides/user/{userId}` | self, ADMIN | A passenger's rides |
| GET | `/rides`, `/rides/active`, `/rides/driver/{driverId}` | ADMIN | Monitoring |

**Rules:** one active ride per passenger, one active ride per driver, pickup and destination must differ, and only the transitions in the state machine ([SEQUENCE-DIAGRAMS.md](SEQUENCE-DIAGRAMS.md)) are allowed.

## Fare & Payment Service (8084)

| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/fares/rates` | public | Rate card and the fare rule |
| POST | `/fares/estimate` | any user, SERVICE | `{ pickup, destination, vehicleType }` or `{ distanceKm, vehicleType }` |
| POST | `/fares/final` | SERVICE, ADMIN | Final fare for a completed ride; creates a PENDING payment; idempotent per ride |
| POST | `/payments` | owner PASSENGER, ADMIN | `{ rideId, paymentMethod: CASH or CARD or WALLET, cardNumber? }`; 200 SUCCESS or 402 FAILED |
| GET | `/payments/ride/{rideId}` | passenger, driver, ADMIN, SERVICE | Payment status |
| GET | `/payments/ride/{rideId}/receipt` | passenger, driver, ADMIN, SERVICE | Receipt (409 until paid) |
| GET | `/payments/{paymentId}` | passenger, driver, ADMIN, SERVICE | Payment record |
| GET | `/payments` | ADMIN | All payments |

**Fare rule (documented in code and returned by `/fares/rates`):**

`fare = base + perKm x distanceKm + perMinute x durationMinutes`, in LKR, rounded half-up to 2 decimals.

| Vehicle | Base | Per km | Per minute |
|---|---|---|---|
| TUK | 100 | 80 | 5 |
| SEDAN | 200 | 120 | 8 |
| VAN | 300 | 150 | 10 |
| SUV | 350 | 170 | 12 |

* Estimate: distance = straight-line (haversine) distance x 1.3 road factor; duration = distance / 25 km/h, rounded up.
* Final: distance reported by the driver (or the estimate if omitted); duration measured by Ride Management from start to completion, minimum 1 minute.
* Simulated payment: CASH and WALLET always succeed; CARD succeeds unless the card number ends in `0002`. Only the last 4 digits are stored. A failed payment can be retried; a successful one cannot be paid again (409).

## Error format and codes (all services)

```json
{ "timestamp": "2026-10-03T08:15:30Z", "status": 409, "error": "Conflict",
  "message": "Invalid ride status transition: cannot move from ASSIGNED to IN_PROGRESS",
  "path": "/api/v1/rides/66fe.../start" }
```

| Code | Meaning |
|---|---|
| 400 | Validation failed (`fieldErrors` lists each field), malformed JSON, bad parameter |
| 401 | No token, or the token is invalid or expired |
| 402 | Simulated card payment declined (body is the FAILED payment) |
| 403 | Wrong role, or not your resource |
| 404 | Resource or endpoint not found |
| 409 | Business rule or state conflict (duplicate email, pending driver, no driver available, invalid transition, already paid) |
| 502 | A dependency rejected the interservice call |
| 503 | A dependency is unavailable |
