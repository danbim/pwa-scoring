#!/usr/bin/env bash +ex

HEAT_ID=$1

curl -v "localhost:8080/contest/$HEAT_ID/contestants"