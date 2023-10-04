#!/usr/bin/env bash

# Dump DB
docker exec mysql bash -c "mysqldump --all-databases -padmin > /tmp/dump.sql" &&
  docker cp mysql:/tmp/dump.sql .etc/docker/dump.sql

# Export Config for End2End Testing
docker exec keycloak bash -c "/opt/keycloak/bin/kc.sh export --file /data/test-realm.json"