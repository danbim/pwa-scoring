#!/usr/bin/env bash +exu

cli/plan-contest.sh 1-A
cli/plan-heat.sh 1-A 2 2 GER-123 GER-321
cli/start-heat.sh 1-A
cli/score-wave.sh 1-A GER-123 9.9
