#!/usr/bin/env bash

set -e

update-ca-certificates

exec "$@"
