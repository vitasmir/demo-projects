 #!/bin/bash
    
    
    
   echo "Starting Infrastructure..."
   cd infrastructure/vault
    read -rsp 'Vault token: ' VAULT_TOKEN
    export VAULT_TOKEN
    echo "Starting Docker Compose with Vault token entered"
    sh docker-compose-vault.sh up --build
    unset VAULT_TOKEN
    echo "Docker infrastructure password vault started."
