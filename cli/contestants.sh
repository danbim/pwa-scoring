#!/usr/bin/env bash +exu

HEAT_ID=$1

curl -v -s \
  -X GET \
  "localhost:8080/contest/heats/$HEAT_ID/contestants"
