#!/usr/bin/env bash +exu

HEAT_ID=$1

echo "Starting heat $HEAT_ID"

curl -v -s \
  -X PUT \
  "localhost:8080/contest/heats/$HEAT_ID?startHeat=true"
