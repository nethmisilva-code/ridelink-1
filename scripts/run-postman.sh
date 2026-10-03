#!/usr/bin/env bash
# Runs the full Postman collection with Newman (needs Node.js) and saves a JUnit report in ./reports
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; mkdir -p "$ROOT/reports"; cd "$ROOT"
npx --yes newman run postman/RideLink.postman_collection.json -e postman/RideLink-Local.postman_environment.json \
  --reporters cli,junit --reporter-junit-export reports/newman-results.xml
