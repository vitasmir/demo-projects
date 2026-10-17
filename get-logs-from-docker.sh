#!/bin/bash

# Script to get logs from a Docker container
mkdir -p "$HOME/jpa-server-logs"
docker logs --follow --tail=0 myapp-jpa-server-backend-1 |
while IFS= read -r line; do
  printf '%s\n' "$line" |
    tee -a "$HOME/jpa-server-logs/myapp-log-$(date +%F).json"
done

