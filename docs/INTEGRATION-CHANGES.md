# Integration changes

What was changed in each member's service so that the four work as one system, and why. Each owner should read their section before the viva: you will be asked about this code.

## Urgent actions for the group

1. **Change these MongoDB Atlas passwords now.** They were committed to Git or hardcoded, so treat them as public:
   * Driver & Vehicle: `.env` committed in the `feature/driver-vehicle-service` history (user `it24101682_db_user`).
   * Ride Management: `application.properties` and `application.yml` committed with the full connection string.
   * Fare & Payment: hardcoded in `MongoConfig.java`.
   * Account: `.env` was gitignored but was included in the shared zip; change it too.
2. **Start the shared repository fresh** from this integrated version, and do not push the old histories that contain those passwords.
3. **Nethmi:** your real Driver & Vehicle code existed only in your local, unpushed `feature/driver-vehicle-service` branch; `main` contained only a README. Push your own work from your machine (see [GIT-WORKFLOW.md](GIT-WORKFLOW.md)).

## Cross-service fixes (affected everyone)

| Problem | Effect | Fix |
|---|---|---|
| Account signed JWTs with SHA-256 of the secret; Driver verified with the raw secret | No token from Account was accepted anywhere else | All four services use jjwt with the raw UTF-8 bytes of one shared `JWT_SECRET` (`JwtKeys`) |
| Token subject was the email | Other services could not link a caller to their account id | `sub` is the account id; `email` and `role` are separate claims |
| Roles: `CUSTOMER` (Account), `PASSENGER` (Driver), `RIDER` (Ride) | Role rules could not match | One role set: `PASSENGER`, `DRIVER`, `ADMIN`, plus `SERVICE` for internal calls. Stored `CUSTOMER` accounts still load and act as `PASSENGER` |
| Vehicle types: `CAR`, `TUK`, `BIKE` (Fare) and `TUK`, `SEDAN`, `VAN`, `SUV` (Driver) | Fares could not be priced for real vehicles | One set: `TUK`, `SEDAN`, `VAN`, `SUV`. The original TUK and CAR rates were kept as TUK and SEDAN |
| Paths `/api/...`, `/api/v1/...`, `/api/rides` | Inconsistent API | Everything under `/api/v1/` |
| Four different error formats; Ride's catch-all leaked exception messages | Inconsistent, and an information leak | One `ApiError` format and one `BaseExceptionHandler` in every service; unexpected errors are logged, not returned |
| Spring Boot 3.3.4 (Ride) versus 4.1.1 (others) | Different APIs and configuration keys | All on Spring Boot 4.1.1, Java 21, springdoc 3.1.1 |
| Each service had its own config style and ports to confirm | Hard to run together | One root `.env` read by all services, fixed ports 8081 to 8084 |

## Account Service (Member 1)

* New `GET /api/v1/accounts/{id}`, allowed for the owner, ADMIN and SERVICE. Driver & Vehicle needs it and was calling a path that did not exist.
* JWT rewritten with jjwt (`JwtService`); the hand-written JWT class depended on Jackson 2, which Spring Boot 4 no longer includes by default.
* Login checks the password before revealing that an account is deactivated. The admin email cannot be registered as a normal account. The admin password comparison is constant-time.
* Admin status change takes a JSON body (`{ "status": "ACTIVE" }`). `RegisterResponse` was renamed `AccountResponse`. `createdAt` and `updatedAt` were added. The password field is named `passwordHash` in code but stored as `password`, so existing documents still load.
* `SecurityConfig` listed `/api/accounts/register`, which did not exist; public paths now match the real endpoints.
* Tests: from 1 context test (which needed a live database) to about 33 real unit tests.

## Driver & Vehicle Service (Member 2)

* Recovered from the unpushed local branch, then kept largely as written (models, repositories, services, business rules and existing tests).
* Profile creation now also requires the account to be ACTIVE (approved by admin), and a driver can only create a profile for their own account.
* Ownership checks: a driver can only change their own profile, location, availability and vehicles.
* Availability: ON_TRIP can only be set by Ride Management (SERVICE) or ADMIN and only from AVAILABLE, using an **atomic MongoDB compare-and-set** (`DriverRepositoryCustomImpl`) so two rides can never reserve the same driver. A driver who is ON_TRIP cannot release themselves.
* Eligibility also requires a location reported in the last 30 minutes (`locationUpdatedAt`), so stale drivers are never matched.
* New `GET /drivers/me` and `PATCH /drivers/{id}`. Latitude and longitude are now required (previously a missing value silently became 0,0).
* Account Service errors now map to 404, 403 or 503 instead of always 409, and the RestClient has timeouts.
* Removed: a duplicate test class in the wrong package (it would always fail), context tests that needed a live database, and the empty `[custom exceptions]` file.

## Ride Management Service (Member 3)

* Package `com.example.ridemanagement` renamed `com.ridelink.ride`. Removed the leftover Node.js files (`app.js`, `db.js`, `package.json`, `node_modules`), the nested duplicate project folder and the hardcoded Atlas URI.
* Added the **ASSIGNED** state required by the brief, and **decline** (ASSIGNED back to REQUESTED). An in-progress ride can no longer be cancelled. Transition rules live in `RideStatus.canTransitionTo`.
* **Interservice integration:**
  * the estimated fare comes from Fare & Payment, no longer from the client
  * assignment asks Driver & Vehicle for eligible drivers and reserves the nearest one (falls back to the next if taken)
  * completion asks Fare & Payment for the final fare
  * cancel, decline and complete release the driver
* Security: the passenger id comes from the token, not the request body. Only the assigned driver can accept, start or complete. `cancelledBy` is taken from the caller's role, not typed by the client.
* Conflicts return 409 (they returned 400), and dependency failures return 503.
* Swagger added. Kept Lombok and the original DTO style, `@Version` optimistic locking and the original repository queries.
* Tests: from 3 to about 53.

## Fare & Payment Service (Member 4)

* Source files were in `src/main/java/config`, `controller` and so on, outside the `com/ridelink` folders, so the package and folder structure did not match. The service was restructured into `com.ridelink.fare`.
* Removed the hardcoded Atlas username and password in `MongoConfig`; the database is configured through `.env`. springdoc 2.3.0 (Spring Boot 3 only) was upgraded to 3.1.1.
* **Fare rule** kept the original idea (base plus per km) and added a per-minute part. It uses `BigDecimal` with half-up rounding. Distance is computed from coordinates (haversine times a 1.3 road factor). The rule is published at `GET /fares/rates`.
* **Final fare** (`POST /fares/final`, SERVICE only) creates a PENDING payment from the completed ride's data and is idempotent per ride.
* **Payments** have PENDING, SUCCESS and FAILED states:
  * a card ending in `0002` is declined (402) and can be retried
  * paying twice is rejected (409)
  * only the card's last 4 digits are stored
  * receipts get a receipt number and are only available after payment
* Security and ownership rules, validation and consistent errors were added. The receipt previously returned distance 0.0; it now shows the real breakdown.
* Tests: from 1 context test to about 33.
