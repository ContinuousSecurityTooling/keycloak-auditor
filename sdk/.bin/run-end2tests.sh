#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SDK_DIR=$SCRIPT_DIR/..

# Kill anything holding port 8080 (kc.sh wrapper + the Java/Quarkus process it spawns).
# pkill on the wrapper alone is not enough — the JVM keeps the port open.
lsof -ti:8080 | xargs kill -9 2>/dev/null || true

# Wait for port 8080 to actually be free before wiping data and starting fresh.
printf 'Waiting for port 8080 to be released'
until ! lsof -ti:8080 > /dev/null 2>&1; do
    printf '.'
    sleep 2
done
printf '\n'

# Wipe H2 data so the new instance gets a clean, unlocked database.
rm -rf "$SDK_DIR/tmp/server/data"

# Start server in the background.
(cd $SDK_DIR && npm ci && npm run end2end:start-server &)

# Wait for Keycloak to become ready (max ~7.5 min: 15 × 30 s).
counter=0
printf 'Waiting for Keycloak server to start'
until curl --output /dev/null --silent --head --fail http://localhost:8080/realms/master/.well-known/openid-configuration; do
    counter=$((counter + 1))
    if [[ "$counter" -gt 15 ]]; then
        printf "\nKeycloak server failed to start. Timeout!\n"
        curl --head --fail http://localhost:8080/realms/master/.well-known/openid-configuration
        exit 1
    fi
    printf '.'
    sleep 30
done
printf '\nKeycloak is up.\n'

(cd $SCRIPT_DIR/.. && npm run end2end:test)