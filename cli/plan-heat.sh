#!/usr/bin/env bash +exu

HEAT_ID=$1
WAVES_COUNTING=$2
JUMPS_COUNTING=$3
shift
shift
shift
RIDER_IDS=$*
RIDER_IDS_ARG=$(printf '%s\n' "$@" | jq -R . | jq -s .)

echo "Planning heat $HEAT_ID with $WAVES_COUNTING waves counting, $JUMPS_COUNTING jumps counting with riders $RIDER_IDS"

curl -v -s \
  -X PUT \
  -H "Content-Type: application/json" \
  --data-binary "{ \"contestants\": { \"riderIds\" : $RIDER_IDS_ARG }, \"rules\" : { \"wavesCounting\" : $WAVES_COUNTING, \"jumpsCounting\": $JUMPS_COUNTING }}" \
  "localhost:8080/contest/heats/$HEAT_ID"
