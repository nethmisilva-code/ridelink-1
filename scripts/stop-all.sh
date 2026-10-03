#!/usr/bin/env bash
# Stops the RideLink services started by start-all.sh
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
for f in "$ROOT"/logs/*.pid; do
  [ -f "$f" ] || continue
  kill "$(cat "$f")" 2>/dev/null && echo "Stopped $(basename "$f" .pid)"
  rm -f "$f"
done
