# Canary deployment with Docker Compose

The rollout runs the current `backend` and a new backend image in parallel.
During the canary phase, Nginx sends 10% of client IPs to the new image and
keeps the assignment stable for that client. This approximates 10% of users;
users sharing one public IP are treated as one client.

Set the image for the new version and start the canary:

```sh
export DATATABLES_CANARY_IMAGE=registry.example.com/datatables-backend:NEW_VERSION
export VAULT_TOKEN=...
./docker-compose-rollout.sh canary
```

Check the canary health endpoint, application metrics, and logs before
promotion. To send all traffic to the new image:

```sh
./docker-compose-rollout.sh promote
```

If the canary is unhealthy, route traffic back to the current backend:

```sh
./docker-compose-rollout.sh rollback
```

The commands use the existing `docker-compose.sh` Vault validation. The
database is shared by both backend versions, so database migrations must be
backward-compatible before starting the canary.
