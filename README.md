# RideLink: Backend Microservices for a Ride-Sharing Platform

IT3130 Application Development, Group Assignment. Four independently executable Spring Boot services that together support passenger and driver accounts, driver preparation, fare estimation, ride booking and assignment, the full ride lifecycle, and simulated payment with receipts.

Swagger UI and the shared Postman collection are the official interfaces. There is no frontend.

## Team and service ownership

| # | Microservice | Port | Primary owner | Database |
|---|---|---|---|---|
| 1 | Account Service | 8081 | _Member 1 name (student ID)_ | `ridelink_accounts` |
| 2 | Driver & Vehicle Service | 8082 | Nethmi Silva (IT24101682) | `ridelink_drivers` |
| 3 | Ride Management Service | 8083 | _Member 3 name (student ID)_ | `ridelink_rides` |
| 4 | Fare & Payment Service | 8084 | _Member 4 name (student ID)_ | `ridelink_payments` |

The whole group owns the architecture, the API contracts and the integration.

## Architecture at a glance

```
                 Postman / Swagger UI  (Bearer JWT from Account Service)
        ┌──────────────┬──────────────────┬───────────────────┬──────────────────┐
        ▼              ▼                  ▼                   ▼
  Account (8081) ◄── Driver & Vehicle (8082) ◄── Ride Management (8083) ──► Fare & Payment (8084)
        │      REST          │           REST         │            REST         │
        ▼                    ▼                        ▼                         ▼
   accounts DB         drivers DB              rides DB                 payments DB
```

* Each service owns its own MongoDB database. No service reads another service's data directly; they exchange stable ids (account id, driver id, ride id) over REST.
* Interservice calls are synchronous REST with short timeouts and clear 503 errors when a dependency is down. Details and the comparison with messaging are in [docs/COMMUNICATION.md](docs/COMMUNICATION.md).
* Security: Account Service issues JWTs. Every service verifies them with the same shared secret and applies role rules (PASSENGER, DRIVER, ADMIN) and ownership rules. Ride Management calls other services with a short-lived SERVICE token.

Full diagrams: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/SEQUENCE-DIAGRAMS.md](docs/SEQUENCE-DIAGRAMS.md).

## Prerequisites

* JDK 21 (`java -version` must show 21 or later). Maven is not needed; each service includes the Maven wrapper.
* MongoDB, one of:
  * MongoDB Community Server running locally on port 27017, or
  * Docker Desktop, then `docker compose up -d` in this folder, or
  * a MongoDB Atlas cluster (set the four `*_MONGODB_URI` values in `.env`).
* Postman (desktop) for the demo. Node.js is optional, only for running the collection from the command line.
* Internet access the first time you build, so Maven can download dependencies.

## Quick start (about 3 minutes, first build takes longer)

**Windows**
1. Start MongoDB.
2. Double-click `start-all.bat` (or run it in a terminal). It creates `.env` from `.env.example`, builds all four services, starts them in the background and waits until each one is healthy.
3. Open Postman, import both files from the `postman` folder, choose the **RideLink Local** environment and run the collection.
4. When finished, double-click `stop-all.bat`.

**macOS / Linux**
```bash
docker compose up -d            # or start your own MongoDB
./scripts/start-all.sh          # add --skip-build after the first run
./scripts/run-postman.sh        # optional: runs the whole collection with Newman
./scripts/stop-all.sh
```

Logs are written to `logs/<service>.log`. If a service does not come up, check that MongoDB is running and look at its log.

## Configuration

All settings come from environment variables. For local runs they are read from one `.env` file in the project root, which every service loads automatically (`spring.config.import`). The file is git-ignored; only `.env.example` is committed, and it contains non-sensitive example values.

| Variable | Used by | Default / example |
|---|---|---|
| `JWT_SECRET` | all four (must be identical, 32+ characters) | example value in `.env.example` |
| `JWT_TTL_MINUTES` | Account | `120` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Account | `admin@ridelink.lk` / `Admin@12345` |
| `ACCOUNT_MONGODB_URI`, `ACCOUNT_MONGODB_DATABASE` | Account | `mongodb://localhost:27017`, `ridelink_accounts` |
| `DRIVER_MONGODB_URI`, `DRIVER_MONGODB_DATABASE` | Driver & Vehicle | `mongodb://localhost:27017`, `ridelink_drivers` |
| `RIDE_MONGODB_URI`, `RIDE_MONGODB_DATABASE` | Ride Management | `mongodb://localhost:27017`, `ridelink_rides` |
| `FARE_MONGODB_URI`, `FARE_MONGODB_DATABASE` | Fare & Payment | `mongodb://localhost:27017`, `ridelink_payments` |
| `ACCOUNT_SERVICE_URL` | Driver & Vehicle | `http://localhost:8081` |
| `DRIVER_SERVICE_URL`, `FARE_SERVICE_URL` | Ride Management | `http://localhost:8082`, `http://localhost:8084` |
| `*_SERVICE_PORT` | each service | 8081, 8082, 8083, 8084 |
| `DRIVER_LOCATION_MAX_AGE_MINUTES` | Driver & Vehicle | `30` |

**Using existing Atlas data:** put the Atlas connection string in the relevant `*_MONGODB_URI` and the existing database name in `*_MONGODB_DATABASE`. Older account documents with role `CUSTOMER` are still readable and are treated as `PASSENGER`.

## Running a single service manually

```bash
cd account-service
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Start-up order: **Account, then Driver & Vehicle, then Fare & Payment, then Ride Management** (Ride depends on Driver and Fare; Driver depends on Account). Services still start if a dependency is down; the affected requests return 503 until it is back.

## Endpoint locations

| Service | Swagger UI | OpenAPI JSON | Health |
|---|---|---|---|
| Account | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs | http://localhost:8081/actuator/health |
| Driver & Vehicle | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs | http://localhost:8082/actuator/health |
| Ride Management | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs | http://localhost:8083/actuator/health |
| Fare & Payment | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs | http://localhost:8084/actuator/health |

In Swagger, log in with `POST /api/v1/auth/login` on port 8081, copy the `token`, press **Authorize** on any service and paste it. The full endpoint list with roles is in [docs/API-REFERENCE.md](docs/API-REFERENCE.md).

## Sample credentials and test data

| What | Value |
|---|---|
| Administrator | `admin@ridelink.lk` / `Admin@12345` (from `.env`, not stored in the database) |
| Passenger | created by the collection as `nadeesha.<runId>@ridelink.test` / `Passw0rd!` |
| Driver | created by the collection as `kasun.<runId>@ridelink.test` / `Passw0rd!` |
| Vehicle | Toyota Prius 2019, White, SEDAN, 4 seats, plate `CAB-<runId>` |
| Trip | Negombo Beach Road (7.2087, 79.8358) to Bandaranaike International Airport (7.1808, 79.8842), service area `Negombo` |
| Declined test card | any number ending in `0002`, for example `4000 0000 0000 0002` |

`<runId>` is generated on every run, so the collection can be run again and again against the same database.

## Testing

**Unit tests** (no database needed): about 150 tests across the four services, covering normal, boundary and failure cases.
```bash
cd ride-management-service && ./mvnw test      # repeat per service
```

**Integrated workflows:** the Postman collection (61 requests, each with assertions) runs every workflow in the brief in order, including 20+ negative scenarios: no token, wrong role, invalid input, duplicate email, pending driver, no available driver, invalid status transitions, declined card and double payment. Run it in the Postman Collection Runner, with `run-postman.bat`, or with `./scripts/run-postman.sh`.

**CI:** `.github/workflows/ci.yml` builds and tests all four services on every push and pull request to `main`, `develop` and `feature/**`. `.github/workflows/integration.yml` (run manually from the Actions tab) starts MongoDB and all four services and runs the Postman collection with Newman.

Details: [docs/TESTING.md](docs/TESTING.md).

## Five-minute demo path

1. Show `start-all.bat` output with all four services UP, and Swagger for each.
2. Run the Postman collection in the Runner and show all tests passing.
3. Walk through the folders: register and approve a driver, driver goes AVAILABLE, fare estimate, ride REQUESTED then ASSIGNED (nearest driver), ACCEPTED, IN_PROGRESS, COMPLETED with final fare, declined card then cash payment, receipt.
4. Show two negative cases live in Swagger: starting a ride before it is accepted (409) and calling an admin endpoint with a passenger token (403).
5. Show the CI run in GitHub Actions.

More detail and likely viva questions: [docs/VIVA-GUIDE.md](docs/VIVA-GUIDE.md).

## Project structure

```
RideLink/
├── account-service/            Member 1
├── driver-vehicle-service/     Member 2
├── ride-management-service/    Member 3
├── fare-payment-service/       Member 4
├── postman/                    collection + local environment
├── docs/                       architecture, sequences, communication, API, testing, Git, viva
├── scripts/                    start/stop/run-postman (PowerShell and bash)
├── .github/workflows/          CI and integration workflows
├── docker-compose.yml          optional local MongoDB
├── .env.example                configuration template (copy to .env)
└── start-all.bat / stop-all.bat / run-postman.bat
```

## Documentation

* [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): service boundaries, data ownership, microservices versus monolith, framework choice
* [docs/SEQUENCE-DIAGRAMS.md](docs/SEQUENCE-DIAGRAMS.md): onboarding, booking and payment sequences
* [docs/COMMUNICATION.md](docs/COMMUNICATION.md): every interservice interaction and why it uses REST
* [docs/API-REFERENCE.md](docs/API-REFERENCE.md): all endpoints, roles and error codes
* [docs/TESTING.md](docs/TESTING.md): unit tests, Postman scenarios and CI evidence
* [docs/GIT-WORKFLOW.md](docs/GIT-WORKFLOW.md): branching strategy and how to commit this work
* [docs/INTEGRATION-CHANGES.md](docs/INTEGRATION-CHANGES.md): what changed in each service during integration, and why
* [docs/VIVA-GUIDE.md](docs/VIVA-GUIDE.md): demo script and practice questions
