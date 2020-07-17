#!/bin/bash

# Runs two test backend. The one on 2020 responds with "blue", the one on 3030 responds with "green"
# Both expose prometheus metrics on /prometheus

# Build backend
../mvnw package -f ../test-backend/pom.xml

# trap ctrl-c and call ctrl_c()
trap ctrl_c SIGINT

function ctrl_c {
  # Kill blue server
  echo "Killing blue server"
  kill $PID_BLUE
  exit 0
}

# Start blue on 2020
java -Xmx32M -Dmicronaut.application.name=blue -Dmicronaut.server.port=2020 -Dservice.failure-percentage=50 -jar ../test-backend/target/test-backend-*.jar &
PID_BLUE=$!

# Start green on 3030
java -Xmx32M -Dmicronaut.application.name=green -Dmicronaut.server.port=3030 -Dservice.failure-percentage=50 -jar ../test-backend/target/test-backend-*.jar
