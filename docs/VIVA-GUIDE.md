# Viva guide

## Before the viva (day before and 15 minutes before)

* [ ] Run `start-all.bat` once the day before so all dependencies are downloaded. On the day, use `start-all.bat -SkipBuild` for a fast start.
* [ ] MongoDB running locally (or Docker). Do not depend on campus Wi-Fi reaching Atlas.
* [ ] Postman open with the collection and the **RideLink Local** environment selected; one full Runner pass done and green.
* [ ] Four Swagger tabs open (ports 8081 to 8084), the GitHub Actions page open with a green run, and the repository open on the `v1.0.0` tag.
* [ ] Every member can start the system and explain the flow end to end, not only their own service.

## Demo script (about 6 minutes)

1. **Architecture (1 minute):** four services, four databases, ids only across boundaries, REST with SERVICE tokens. Show the diagram in `docs/ARCHITECTURE.md`.
2. **Run the collection (1 minute):** Collection Runner, all tests green.
3. **Walk the happy path (2 minutes):** in Postman, show the responses for:
   * driver registered as PENDING, then approved by the admin
   * driver AVAILABLE
   * the estimate
   * ride REQUESTED, then ASSIGNED (show `driverId` and that the driver is now ON_TRIP)
   * ACCEPTED, IN_PROGRESS, then COMPLETED with `finalFare` and `paymentId`
   * declined card (402), cash payment (200), receipt
4. **Negative cases live in Swagger (1 minute):** start a ride that is only ASSIGNED (409); call `GET /api/v1/admin/accounts` with a passenger token (403); call without a token (401).
5. **Resilience (optional, 30 seconds):** stop Fare & Payment, try to create a ride, show the 503 message, start it again.
6. **Quality (30 seconds):** unit test output, CI run, pull request history.

## Questions for the whole group

**Why microservices instead of a monolith?**
Four business capabilities with their own data and owners, which the brief requires to be developed independently and scale separately. We pay for it with network calls and no shared transactions. We handled that with timeouts, clear 503 errors, idempotent final fares and best-effort driver release.

**How do you stop one service reading another's data?**
Each service connects only to its own database. Others are reached through their REST API using stable ids. Example: Ride stores the driver id and asks Driver & Vehicle for availability; it never queries the drivers collection.

**Why synchronous REST and not a message queue?**
Each interaction needs an answer before replying to the user: fare for the new ride, a driver for the assignment, a final fare at completion. Releasing the driver after a ride is the exception; a `RideCompleted` event through an outbox and a queue would be the improvement.

**What happens if Fare & Payment is down?**
Ride creation and completion return 503 with a clear message. Nothing is saved half-way, and the ride stays IN_PROGRESS. Because `/fares/final` is idempotent per ride, retrying is safe.

**How does authentication work across services?**
Account Service signs a JWT with the shared secret. Every service verifies the signature and expiry in `JwtAuthFilter`, then applies role rules (`@PreAuthorize`) and ownership rules in the service layer. Ride Management uses its own short-lived SERVICE token for internal calls.

**Weakness of your security design?**
A shared HMAC secret means any service could mint tokens. Production would use RS256 (only Account holds the private key) and OAuth2 client credentials for services, plus an API gateway.

**How is the driver chosen?**
Driver & Vehicle returns drivers who are:
* AVAILABLE
* in the same service area
* with an active vehicle of the requested type
* with a location reported in the last 30 minutes

Ride sorts them by straight-line distance to the pickup, then by the freshest location, then by id. It reserves the first with an atomic update; if someone else got that driver first, it tries the next.

**Can two rides get the same driver?**
No. Reservation is a single MongoDB update with the condition `availability = AVAILABLE`, so only one succeeds. Ride also keeps `@Version` optimistic locking on its own documents.

**How did you test?**
About 150 unit tests with Mockito and no database, covering normal, boundary and failure cases. The Postman collection (61 requests) covers every workflow and over 20 negative cases. CI builds and tests all four services on each push and pull request.

**Branching strategy?**
Feature branches into `develop` through reviewed pull requests, then `develop` into `main`, which is always demonstrable, tagged `v1.0.0`.

## Questions by service

**Account (Member 1)**
* Why are drivers PENDING? So an admin approves them before they can create a driver profile and receive rides.
* Where is the password stored? Only as a BCrypt hash. It is never returned.
* Why is the admin not in the database? It is a configured break-glass account from `.env`; any real system would store admins with proper user management.
* What is in the token? `sub` (account id), `email`, `role`, `iss`, `iat`, `exp`; two hours by default.

**Driver & Vehicle (Member 2)**
* What does the Account call check? That the account exists, has the DRIVER role and is ACTIVE. It forwards the driver's token, and maps errors to 404, 403 or 503.
* When can a driver go AVAILABLE? Only with an active vehicle and a reported location.
* Why the 30-minute location rule? Stale drivers (app closed, phone off) must not be matched.
* Why can a driver not set ON_TRIP or release themselves? Those are owned by the ride lifecycle; letting drivers do it would break matching.

**Ride Management (Member 3)**
* Show the state machine: `RideStatus.canTransitionTo`; every other change returns 409.
* Why is the passenger id not in the request body? It comes from the token, so nobody can book for someone else.
* Why can an in-progress ride not be cancelled? It must be completed so it can be priced and paid.
* What if the driver service fails while releasing the driver? The ride change is kept and a warning is logged; an admin can reset the driver. This is the known limitation solved by an outbox and a queue.
* How is duration measured? Server-side, from `startedAt` to completion, minimum 1 minute.

**Fare & Payment (Member 4)**
* Explain the rule: base plus per km plus per minute, by vehicle type, using `BigDecimal` and half-up rounding. Estimates use haversine distance times 1.3 and 25 km/h.
* Why `BigDecimal`? Floating point cannot represent money exactly.
* Why is the final fare idempotent? Ride may retry after a timeout; the same ride must never get two charges.
* How is a payment failure simulated? CARD numbers ending in `0002` are declined (402); the passenger can retry with another method; paying again after success returns 409.
* Why store only the last 4 digits? Card data must never be stored in full.

## Reflection points (to show critical thinking)

* With more time: an outbox and RabbitMQ for driver release and notifications, RS256 tokens, an API gateway, integration tests with Testcontainers, real routing distance.
* What went wrong: services were built in isolation with different JWT keys, role names, vehicle types and Spring Boot versions. Agreeing API contracts and conventions on day one would have avoided most integration fixes.
