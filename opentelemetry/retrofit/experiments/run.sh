#!/bin/env bash

set -euo pipefail

server >server.log 2>&1 &
trap "pkill server" EXIT

until test -S main.sock; do
    sleep 1
done

for events in ${EVENTS:-100 1000 10000 100000}; do
  client-v1 -e "$events"
  client-v2 -e "$events"
  client-v2 -e "$events" -w
  client-v3 -e "$events"
  client-v4 -e "$events"
  client-v5 -e "$events"
done
