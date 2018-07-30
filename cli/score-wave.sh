#!/usr/bin/env bash +exu

HEAT_ID=$1
RIDER_ID=$2
POINTS=$3

echo "Scoring wave in heat $HEAT_ID for rider $RIDER_ID: $POINTS points"

curl -v -s \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "{ \"points\" : $POINTS }" \
  "localhost:8080/contest/heats/$HEAT_ID/waveScores/$RIDER_ID"
