#!/bin/bash

# requires https://stedolan.github.io/jq/download/

# config
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=master
KEYCLOAK_CLIENT_ID=admin-cli-end2end-testing
KEYCLOAK_CLIENT_SECRET=tIr3tYSCr0uEya2xx9vl9xnMgZTN4e0Y

export TKN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
 -H "Content-Type: application/x-www-form-urlencoded" \
 -d "client_id=${KEYCLOAK_CLIENT_ID}" \
 -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
 -d 'grant_type=client_credentials' \
 -d 'scope=profile' | jq -r '.access_token')

curl -s -X GET "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/auditing/users" \
-H "Accept: application/json" \
-H "Authorization: Bearer $TKN" | jq .
