#!/usr/bin/env bash +ex

HEAT_ID=$1
shift
RIDER_IDS=$*
RIDER_IDS_ARG=$(printf '%s\n' "$@" | jq -R . | jq -s .)

echo "Starting heat $HEAT_ID with riders $RIDER_IDS"

curl -v "localhost:8080/contest/$HEAT_ID" -H "Content-Type: application/json" --data-binary "{ \"riderIds\" : $RIDER_IDS_ARG }"