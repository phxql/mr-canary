#!/bin/bash

# Runs two simple http servers. The one on 2020 responds with "Primary!", the one on 3030 responds with "Canary!"

# trap ctrl-c and call ctrl_c()
trap ctrl_c SIGINT

function ctrl_c {
  echo "Killing primary server"
  kill $PID_PRIMARY
  exit 0
}

python3 -m http.server --bind 127.0.0.1 --directory primary 2020 &
PID_PRIMARY=$!
python3 -m http.server --bind 127.0.0.1 --directory canary 3030
