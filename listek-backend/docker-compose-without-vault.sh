#!/bin/sh
export DB_NAME=listek
export DB_PASSWORD=password
export DB_URL=jdbc:postgresql://postgres:5432/listek
export DB_USERNAME=postgres

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec docker compose -f "${script_dir}/docker-compose.ymls" "$@"
unset DB_URL
unset DB_USERNAME
unset DB_PASSWORD
unset DB_NAME
