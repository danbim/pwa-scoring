#!/usr/bin/env bash +exu

HEAT_ID=$1

echo "Ending heat $HEAT_ID"

curl -v \
  -X PUT \
  "localhost:8080/contest/heats/$HEAT_ID?endHeat=true"