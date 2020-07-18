#!/usr/bin/env bash

# Uses https://github.com/giltene/wrk2

PARAMS="-c 1 -d 10s -t 1 -R 10"

wrk $PARAMS http://localhost:2020/ &
wrk $PARAMS http://localhost:3030/ &
