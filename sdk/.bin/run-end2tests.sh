#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SDK_DIR=$SCRIPT_DIR/..
# Start server
(cd $SDK_DIR && npm ci && npm run end2end:start-server &)
# For debugging purposes to make sure the image was started
counter=0
printf 'Waiting for Keycloak server to start'
until $(curl --output /dev/null --silent --head --fail http://localhost:8080/realms/master/.well-known/openid-configuration); do
    printf '.'
    sleep 30
    if [[ "$counter" -gt 15 ]]; then
        printf "Keycloak server failed to start. Timeout!"
        curl --head --fail http://localhost:8080/realms/master/.well-known/openid-configuration
        exit 1
    fi
    counter=$((counter + 1))
done

(cd $SCRIPT_DIR/.. && npm run end2end:test)