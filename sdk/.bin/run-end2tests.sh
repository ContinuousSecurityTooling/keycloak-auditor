#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SDK_DIR=$SCRIPT_DIR/..
# Copy SPI
(cd $SDK_DIR && cp ../spi/target/keycloak-auditor-spi.jar sdk/tmp/server/providers/ tmp/server/providers/)
# For debugging purposes to make sure the image was started
counter=0
printf 'Waiting for Keycloak server to start'
until $(curl --output /dev/null --silent --head --fail http://localhost:8080/realms/master/.well-known/openid-configuration); do
    printf '.'
    sleep 5
    if [[ "$counter" -gt 24 ]]; then
        printf "Keycloak server failed to start. Timeout!"
        curl --head --fail http://localhost:8080/realms/master/.well-known/openid-configuration
        exit 1
    fi
    counter=$((counter + 1))
done

(cd $SCRIPT_DIR/.. && npm run end2end:test)