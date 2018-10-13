#!/usr/bin/env bash +exu

cli/plan-contest.sh \
	1-A 1-B \
	2-A 2-B \
	3-A 3-B \
	4-A 4-B \
	5-A 5-B

cli/plan-heat.sh 1-A 2 2 GER-123 GER-321
cli/plan-heat.sh 1-B 2 2 USA-1 K-516

