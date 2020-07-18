#!/usr/bin/env bash

# Uses https://github.com/giltene/wrk2

wrk -c 1 -d 10s -t 1 -R 10 http://localhost:8080/
