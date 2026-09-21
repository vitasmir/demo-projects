#!/bin/bash
set -e

echo "running listek-frontend build"
cd "../listek-frontend"
npm install
npm run build

echo "running listek-manager build"
cd "../listek-manager"
npm install
npm run build


echo "running listek-admin build"
cd "../listek-admin"
mvn clean install -DskipTests

echo "running listek-backend build"
cd "../listek-backend"
mvn clean install -DskipTests



echo "Starting Docker Compose services with rebuild..."
read -rsp 'Vault token: ' VAULT_TOKEN
echo
export VAULT_TOKEN
trap 'unset VAULT_TOKEN' EXIT
sh "docker-compose-vault.sh" up --build
