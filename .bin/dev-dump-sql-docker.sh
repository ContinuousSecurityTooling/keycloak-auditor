#!/usr/bin/env bash

docker exec mysql bash -c "mysqldump --all-databases -padmin > /tmp/dump.sql" &&
  docker cp mysql:/tmp/dump.sql .etc/docker/dump.sql
