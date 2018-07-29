#!/usr/bin/env bash +exu

HEAT_ID=$1
RIDER_ID=$2
JUMP_TYPE=$3
POINTS=$4

echo "Scoring $JUMP_TYPE jump in heat $HEAT_ID for rider $RIDER_ID: $POINTS points"

curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "{ \"points\" : $POINTS, \"jumpType\" : \"$JUMP_TYPE\" }" \
  "localhost:8080/contest/heats/$HEAT_ID/jumpScores/$RIDER_ID"