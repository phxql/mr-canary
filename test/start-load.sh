#!/usr/bin/env bash

# Uses https://github.com/giltene/wrk2

wrk -c 1 -d 1m -t 1 -R 2 http://localhost:8080/
