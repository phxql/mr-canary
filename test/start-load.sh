#!/usr/bin/env bash

# Uses https://github.com/giltene/wrk2

wrk --connections 1 --duration 1m --threads 1 --rate 2 --header "Connection: close" http://localhost:8080/
