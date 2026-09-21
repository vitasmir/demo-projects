#!/bin/sh
set -eu

reconcile_secret() {
  secret_path="$1"

  DB_URL="$(vault kv get -field=DB_URL "$secret_path")"
  DB_USERNAME="$(vault kv get -field=DB_USERNAME "$secret_path")"
  DB_PASSWORD="$(vault kv get -field=DB_PASSWORD "$secret_path")"
  DB_NAME="$(vault kv get -field=DB_NAME "$secret_path")"

  case "$DB_URL" in
    jdbc:postgresql://*) ;;
    *)
      echo "DB_URL in $secret_path must start with jdbc:postgresql://" >&2
      exit 1
      ;;
  esac

  if [ -z "$DB_NAME" ] || [ -z "$DB_USERNAME" ] || [ -z "$DB_PASSWORD" ]; then
    echo "DB_URL, DB_USERNAME and DB_PASSWORD must be set in $secret_path" >&2
    exit 1
  fi

  role_exists="$(psql -At -h /var/run/postgresql -U postgres -d postgres \
    -v db_username="$DB_USERNAME" <<'SQL'
SELECT 1 FROM pg_roles WHERE rolname = :'db_username';
SQL
)"

  if [ "$role_exists" = "1" ]; then
    psql -v ON_ERROR_STOP=1 -h /var/run/postgresql -U postgres -d postgres \
      -v db_username="$DB_USERNAME" -v db_password="$DB_PASSWORD" <<'SQL'
ALTER ROLE :"db_username" LOGIN PASSWORD :'db_password';
SQL
  else
    psql -v ON_ERROR_STOP=1 -h /var/run/postgresql -U postgres -d postgres \
      -v db_username="$DB_USERNAME" -v db_password="$DB_PASSWORD" <<'SQL'
CREATE ROLE :"db_username" LOGIN PASSWORD :'db_password';
SQL
  fi

  database_exists="$(psql -At -h /var/run/postgresql -U postgres -d postgres \
    -v db_name="$DB_NAME" <<'SQL'
SELECT 1 FROM pg_database WHERE datname = :'db_name';
SQL
)"

  if [ "$database_exists" != "1" ]; then
    createdb -h /var/run/postgresql -U postgres -O "$DB_USERNAME" "$DB_NAME"
  fi

  PGPASSWORD="$DB_PASSWORD" psql -v ON_ERROR_STOP=1 -h postgres \
    -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1" >/dev/null
}

reconcile_secret secrets/listek
reconcile_secret secrets/listek-admin

cp /config/pg_hba.scram.conf /auth/pg_hba.conf
psql -v ON_ERROR_STOP=1 -h /var/run/postgresql -U postgres -d postgres \
  -c "SELECT pg_reload_conf()" >/dev/null