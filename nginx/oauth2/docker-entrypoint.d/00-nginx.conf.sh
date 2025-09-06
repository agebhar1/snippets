#!/bin/sh

set -e

test -d /etc/nginx/partials || mkdir /etc/nginx/partials

grep -q 'include /etc/nginx/partials/load_module;' /etc/nginx/nginx.conf || \
  sed --in-place -e '1iinclude /etc/nginx/partials/load_module;\' /etc/nginx/nginx.conf

exit 0
