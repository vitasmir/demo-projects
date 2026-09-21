 #!/bin/bash
    
    echo "running ng build"
    cd ../myapp-client
    ng build

    cd ../myapp-jpa-server
    mvn clean install -DskipTests
    

    # Filebeat refuses configuration files writable by group or other users.
    chmod go-w ../filebeat/filebeat.yml

    echo "Starting Docker Compose services with rebuild..."
    read -rsp 'Vault token: ' VAULT_TOKEN
    export VAULT_TOKEN
    sh docker-compose-vault.sh up --build
    unset VAULT_TOKEN
