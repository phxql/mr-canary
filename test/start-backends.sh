#!/usr/bin/env bash

# Runs two test backend. The one on 2020 responds with "blue", the one on 3030 responds with "green"
# Both expose prometheus metrics on /prometheus
# If start with '--build', it will build the backends. It will also build the backend if it hasn't been built yet.

if [ "$1" == "--build" ] || [ ! -f "../test-backend/target/test-backend-0.1.jar" ]; then
  # Build backend
  ../mvnw package -f ../test-backend/pom.xml
fi

# trap ctrl-c and call ctrl_c()
trap ctrl_c SIGINT

function ctrl_c {
  # Kill blue server
  echo "Killing blue server"
  kill $PID_BLUE
  exit 0
}

# Start blue on 2020
java -Xmx32M -Dmicronaut.application.name=blue -Dmicronaut.server.port=2020 -Dservice.failure-percentage=50 -jar ../test-backend/target/test-backend-0.1.jar &
PID_BLUE=$!

# Start green on 3030
java -Xmx32M -Dmicronaut.application.name=green -Dmicronaut.server.port=3030 -Dservice.failure-percentage=1 -jar ../test-backend/target/test-backend-0.1.jar
