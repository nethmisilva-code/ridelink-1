#!/usr/bin/env bash
# Builds (unless --skip-build) and starts all four RideLink services in the background,
# then waits until each one reports healthy. Logs go to ./logs/<service>.log
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; LOGS="$ROOT/logs"; mkdir -p "$LOGS"
SKIP_BUILD=false; [ "${1:-}" = "--skip-build" ] && SKIP_BUILD=true
# Start order follows the dependencies: Ride calls Driver and Fare; Driver calls Account.
SERVICES="account-service:8081 driver-vehicle-service:8082 fare-payment-service:8084 ride-management-service:8083"

command -v java >/dev/null || { echo "Java was not found. Install JDK 21."; exit 1; }
[ -f "$ROOT/.env" ] || { cp "$ROOT/.env.example" "$ROOT/.env"; echo "Created .env from .env.example"; }

for entry in $SERVICES; do
  name="${entry%%:*}"; port="${entry##*:}"; dir="$ROOT/$name"
  if [ "$SKIP_BUILD" = false ]; then
    echo "Building $name ..."; (cd "$dir" && chmod +x mvnw && ./mvnw -q -DskipTests package)
  fi
  jar=$(ls "$dir"/target/*.jar 2>/dev/null | head -1)
  [ -n "$jar" ] || { echo "No jar for $name. Run without --skip-build."; exit 1; }
  (cd "$dir" && { nohup java -jar "$jar" > "$LOGS/$name.log" 2>&1 & echo $! > "$LOGS/$name.pid"; })
  echo "Started $name (pid $(cat "$LOGS/$name.pid")) on port $port"
done

echo "Waiting for services to become healthy ..."
for entry in $SERVICES; do
  name="${entry%%:*}"; port="${entry##*:}"; ok=false
  for _ in $(seq 1 90); do
    if curl -fs "http://localhost:$port/actuator/health" | grep -q '"UP"'; then ok=true; break; fi
    sleep 2
  done
  if $ok; then printf "  %-26s UP    http://localhost:%s/swagger-ui.html\n" "$name" "$port"
  else printf "  %-26s NOT UP  see logs/%s.log (is MongoDB running?)\n" "$name" "$name"; fi
done
echo "Stop everything with: scripts/stop-all.sh"
