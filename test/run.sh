#!/bin/bash

# Runs two simple http servers. The one on 2020 responds with "blue", the one on 3030 responds with "green"

# trap ctrl-c and call ctrl_c()
trap ctrl_c SIGINT

function ctrl_c {
  echo "Killing blue server"
  kill $PID_BLUE
  exit 0
}

python3 -m http.server --bind 127.0.0.1 --directory blue 2020 &
PID_BLUE=$!
python3 -m http.server --bind 127.0.0.1 --directory green 3030
