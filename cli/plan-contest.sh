#!/usr/bin/env bash +exu

HEAT_IDS=$*
HEAT_IDS_ARG=$(printf '%s\n' "$@" | jq -R . | jq -s .)

echo "Planning contest with heats $HEAT_IDS"

curl -v -s \
  -X PUT \
  -H "Content-Type: application/json" \
  --data-binary "{ \"heatIds\" : $HEAT_IDS_ARG }" \
  "localhost:8080/contest/heats"
