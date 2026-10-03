# Interservice communication

## Interactions

| # | Caller to callee | Endpoint | When | Interface | Auth | If the callee fails |
|---|---|---|---|---|---|---|
| 1 | Driver & Vehicle to Account | `GET /api/v1/accounts/{id}` | Driver creates a profile | Sync REST | Caller's own JWT forwarded | 404 account unknown, 403 not allowed, 503 unavailable; no profile created |
| 2 | Ride to Fare | `POST /api/v1/fares/estimate` | Passenger creates a ride | Sync REST | SERVICE token | 503; ride not created |
| 3 | Ride to Driver | `GET /api/v1/drivers/available` | Assignment | Sync REST | SERVICE token | 503; ride stays REQUESTED |
| 4 | Ride to Driver | `PATCH /api/v1/drivers/{id}/availability` ON_TRIP | Reserving the chosen driver | Sync REST | SERVICE token | 409 tries the next driver; 503 keeps ride REQUESTED |
| 5 | Ride to Fare | `POST /api/v1/fares/final` | Driver completes the ride | Sync REST, idempotent | SERVICE token | 503; ride stays IN_PROGRESS and can be retried safely |
| 6 | Ride to Driver | `PATCH /api/v1/drivers/{id}/availability` AVAILABLE | Ride completed, cancelled or declined | Sync REST, best effort | SERVICE token | Logged warning; the ride state change is kept |

All calls use `RestClient` with a 2 second connect timeout and a 5 second read timeout, so a slow dependency fails fast instead of hanging the caller.

## Why synchronous REST for each interaction

* **1. Account check:** the driver is waiting for a yes or no. The profile must not exist unless the account is a real, ACTIVE driver, so an immediate answer is required.
* **2. Fare estimate:** the passenger needs the price in the same response that creates the ride, and pricing must come from the one service that owns it.
* **3 and 4. Matching and reservation:** assignment is a request-response decision. Reserving with a conditional update (only AVAILABLE can become ON_TRIP) prevents two rides taking the same driver; a 409 tells Ride to try the next candidate immediately.
* **5. Final fare:** completing a ride without a price would leave an unpayable ride. Making the call idempotent per `rideId` turns the main risk of synchronous calls (retry after a timeout) into a safe operation.
* **6. Driver release:** this one would suit messaging best, because the ride should complete even if the driver service is briefly down. It is synchronous and best effort here to avoid running a broker for the demo; this is listed as the first improvement.

## Alternatives considered

| | Synchronous REST (chosen) | Asynchronous messaging (RabbitMQ or Kafka) | gRPC |
|---|---|---|---|
| Fits request-response decisions | Yes | Awkward: needs reply queues or polling | Yes |
| Coupling in time | Both services must be up | Producer and consumer decoupled | Both must be up |
| Failure handling | Timeouts, clear 503, idempotent retry | Broker buffers; retries and dead-letter queues | Deadlines, status codes |
| Contracts | OpenAPI, JSON, easy to test in Postman and Swagger | Message schemas, harder to demo | Protobuf, strongly typed, not testable in Swagger |
| Extra infrastructure | None | A broker to run and monitor | Code generation and HTTP/2 tooling |
| Best fit in RideLink | Interactions 1 to 5 | Interaction 6 and future notifications (RideCompleted event) | High-volume internal calls such as live location |

**How interaction 6 would look with messaging:** Ride saves the completed ride and a `RideCompleted` event in the same database write (outbox), a relay publishes it to a queue, and Driver & Vehicle consumes it and sets the driver AVAILABLE. Delivery is guaranteed even if the Driver service is down for minutes.

## Service-to-service authentication

Ride Management mints a short-lived JWT (5 minutes, cached and renewed one minute before expiry) with `sub = ride-management-service` and `role = SERVICE`, signed with the shared secret. Driver & Vehicle and Fare & Payment accept SERVICE only on the endpoints Ride needs, and only SERVICE or ADMIN can set a driver ON_TRIP or create a final fare, so a passenger or driver cannot fake either.

Driver & Vehicle forwards the driver's own token to Account Service instead, because the check is "can this user see this account", which keeps the user's identity in the call.

## Contracts

* REST contracts are documented by OpenAPI in each service (`/v3/api-docs`, Swagger UI).
* Each client has its own small DTO for the fields it consumes (`AccountSummary`, `AvailableDriver`, `FareQuote`, `FinalFare`) with unknown fields ignored, so adding fields never breaks a caller.
* Enum values shared across services: vehicle types `TUK, SEDAN, VAN, SUV`; availability `OFFLINE, AVAILABLE, ON_TRIP`; roles `PASSENGER, DRIVER, ADMIN, SERVICE`.
