#!/bin/env bash

set -euo pipefail

cmake --build build

logfile=$(date +"%Y%m%dT%H%M%S%z").log

exec 1>>"${logfile}"
exec 2>&1

for events in 100 1000 10000 100000; do
  echo "---< $events >---"
  echo "---<< 1 >>---"
  /usr/bin/time build/client-v1 -e $events
  echo "---<< 2a >>---"
  /usr/bin/time build/client-v2 -e $events
  echo "---<< 2b >>---"
  /usr/bin/time build/client-v2 -e $events -w
  echo "---<< 3 >>---"
  /usr/bin/time build/client-v3 -e $events
  echo "---<< 4 >>---"
  /usr/bin/time build/client-v4 -e $events
  echo "---<< 5 >>---"
  /usr/bin/time build/client-v5 -e $events
done
