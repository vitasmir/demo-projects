#!/bin/sh
set -eu

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
export COMPOSE_EXTRA_FILE="${script_dir}/docker-compose.rollout.yml"

case "${1:-}" in
  canary)
    shift
    export CANARY_BACKEND_PERCENTAGE="${CANARY_BACKEND_PERCENTAGE:-10}"
    exec "${script_dir}/docker-compose.sh" up -d --no-build backend-canary myapp-client "$@"
    ;;
  promote)
    shift
    export CANARY_BACKEND_PERCENTAGE=100
    exec "${script_dir}/docker-compose.sh" up -d --no-build backend-canary myapp-client "$@"
    ;;
  rollback)
    shift
    export CANARY_BACKEND_PERCENTAGE=0
    exec "${script_dir}/docker-compose.sh" up -d --no-build myapp-client "$@"
    ;;
  *)
    echo "Usage: $0 {canary|promote|rollback}" >&2
    echo "Required: VAULT_TOKEN and DATATABLES_CANARY_IMAGE" >&2
    exit 2
    ;;
esac
