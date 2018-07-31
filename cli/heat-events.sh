#!/usr/bin/env bash +exu

HEAT_ID=$1

echo "Subscribing to events of heat $HEAT_ID"

curl -v -s \
  -X GET \
  "localhost:8080/contest/heats/$HEAT_ID/events"
