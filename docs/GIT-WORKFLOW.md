# Git workflow

## Branching strategy

* `main`: always integrated and demonstrable. Changes arrive only through reviewed pull requests. Tagged `v1.0.0` for submission.
* `develop`: integration branch where the four services come together.
* `feature/<service>-<topic>`: one branch per piece of work, for example `feature/ride-assignment`, `feature/fare-final-fare`.

Flow: feature branch, small meaningful commits, pull request into `develop`, review by at least one other member, CI green, merge. When `develop` is stable, open a pull request from `develop` to `main`.

## Commit messages

Conventional style: `type(scope): summary`

```
feat(ride): assign nearest eligible driver
fix(driver): make ON_TRIP reservation atomic
test(fare): cover declined card and retry
docs(readme): add start-up order and test data
chore(ci): build all four services in a matrix
```

## Moving this integrated code into the shared repository

The integrated project must reach the repository through each member's own identity, because individual Git marks (I3) depend on traceable, authentic history. Do not commit another member's service as yourself, and do not rewrite dates or authors.

1. **Create the repository** (one member): add the root files (`README.md`, `.gitignore`, `.gitattributes`, `.env.example`, `docker-compose.yml`, `.github/`, `scripts/`, `postman/`, `docs/`, `*.bat`), push to `main`, create `develop`, and protect `main` (require a pull request, one review and passing CI).
2. **Each owner adds their own service** on a feature branch, for example `feature/account-service`, in a few logical commits (model and repository; service and rules; controller and validation; security; tests; docs), then opens a pull request into `develop`.
3. **Review each other's pull requests** with real comments (contracts, status codes, tests). Approve and merge.
4. **Integration pull request:** `develop` into `main` once the Postman collection passes end to end.
5. **Tag:** `git tag -a v1.0.0 -m "Assessed version" && git push origin v1.0.0`.

## Before the first push

* Make sure no `.env`, `target/`, `node_modules/` or `.idea/` folders are included (`git status` should show none of them).
* The MongoDB passwords that were committed or hardcoded earlier must be changed in MongoDB Atlas before anything else is pushed (see [INTEGRATION-CHANGES.md](INTEGRATION-CHANGES.md)). Start the shared repository fresh rather than reusing histories that contain those passwords.
* Nethmi's Driver & Vehicle code existed only in her local, unpushed `feature/driver-vehicle-service` branch. She should push her real work from her machine so her history is preserved.
