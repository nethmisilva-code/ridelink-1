# Testing

## Unit tests (per service, no database needed)

Run inside any service folder: `./mvnw test` (Windows: `mvnw.cmd test`). Reports: `target/surefire-reports`.

| Service | Test classes | What they prove |
|---|---|---|
| Account (about 33) | `AuthServiceTest`, `AccountServiceTest`, `JwtServiceTest`, `RoleTest`, `AuthControllerTest` | Passenger ACTIVE and driver PENDING on registration, duplicate and reserved emails rejected, legacy CUSTOMER treated as PASSENGER, login success, wrong password, unknown email, inactive account, admin login, account lookup rules (owner, ADMIN, SERVICE, others 403), password change, token round trip, tampered, foreign, expired and malformed tokens, missing or short secret, 400 validation format |
| Driver & Vehicle (about 35) | `DriverServiceTest`, `VehicleServiceTest`, `DriverControllerTest` | Profile only for an ACTIVE DRIVER account, pending account rejected, own account only, duplicate profile, AVAILABLE needs vehicle and location, ON_TRIP rules, atomic reservation race, ownership checks, eligibility (area, type, active vehicle, stale location excluded), duplicate plate, last active vehicle |
| Ride Management (about 53) | `RideServiceImplTest`, `RideStatusTest`, `NearestDriverSelectorTest`, `RideControllerTest` | Fare taken from Fare service, one active ride per passenger, nearest driver chosen, next driver tried when one is taken, no driver available, admin manual assignment, every allowed and forbidden transition, accept, decline, start, complete with final fare, cancel releases driver, release failure does not block, 503 when a dependency is down, access rules |
| Fare & Payment (about 33) | `FareCalculatorTest`, `FareServiceTest`, `PaymentServiceTest`, `PaymentControllerTest` | Rule per vehicle type, rounding, zero distance, haversine distance, estimate from coordinates or distance, final fare idempotent, card declined (402) then retry, double payment (409), wrong passenger (403), receipt only after success, card masking |

Tests use JUnit 5, Mockito (strict stubs) and AssertJ. Controller tests use standalone MockMvc to check request mapping, validation and the error format without starting Spring or MongoDB, so CI needs no database.

## Integrated workflows (Postman)

`postman/RideLink.postman_collection.json` with environment `postman/RideLink-Local.postman_environment.json`. 61 requests, each with assertions; ids and tokens are passed between requests automatically. A new `runId` is generated per run, so it can be repeated against the same database.

| Folder | Brief workflow | Highlights |
|---|---|---|
| 0. Health checks | | All four services UP |
| 1. Account and access | 1 | Register passenger and driver, pending driver blocked (409), admin approves, logins, profile |
| 2. Driver preparation | 2 | Profile verified with Account Service, AVAILABLE without vehicle (409), vehicle, location, AVAILABLE, eligible list |
| 3. Fare estimation | 3 | Rate card, estimate |
| 4. Ride request and assignment | 4 | REQUESTED with estimated fare, ASSIGNED to nearest driver, driver now ON_TRIP |
| 5. Ride lifecycle | 5 | Start before accept (409), ACCEPTED, IN_PROGRESS, cancel in progress (409), COMPLETED with final fare, driver AVAILABLE again |
| 6. Completion and payment | 6 | PENDING payment, receipt before payment (409), declined card (402), cash (200), double payment (409), receipt |
| 7. Negative and alternate | 5, 7 | No token (401), passenger on admin endpoint (403), wrong driver (403), invalid input (400), duplicate email (409), bad login (401), unknown vehicle type (400), complete twice (409), unknown ride (404), decline and reassign, cancel after assignment releases driver, no driver available (409) then cancel |
| 8. Clean up | | Driver goes OFFLINE |

Run it in Postman (Collection Runner), or from the command line with Newman (`run-postman.bat` or `./scripts/run-postman.sh`), which also writes `reports/newman-results.xml`. For the report, export the Runner summary or the Newman output as evidence.

## Continuous integration

* `.github/workflows/ci.yml`: a matrix job per service (`./mvnw clean verify`) on every push to `main`, `develop` and `feature/**`, on every pull request to `main` and `develop`, and on demand. Test reports are uploaded as artifacts.
* `.github/workflows/integration.yml` (manual): starts MongoDB and all four services on the runner and runs the full Postman collection with Newman; logs and the JUnit report are uploaded as evidence.

For the report, screenshot a green CI run and a green integration run from the Actions tab.
