#!/usr/bin/env bash +exu

HEAT_ID=$1
shift
RIDER_IDS=$*
RIDER_IDS_ARG=$(printf '%s\n' "$@" | jq -R . | jq -s .)

echo "Planning heat $HEAT_ID with riders $RIDER_IDS"

curl -v -s \
  -X PUT \
  -H "Content-Type: application/json" \
  --data-binary "{ \"riderIds\" : $RIDER_IDS_ARG }" \
  "localhost:8080/contest/heats/$HEAT_ID"
