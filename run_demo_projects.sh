 #!/bin/bash
    
    echo "Starting DATATABLES..."
    cd myapp-jpa-server
    read -rsp 'Vault token: ' VAULT_TOKEN
    export VAULT_TOKEN
    echo "Starting Docker Compose with Vault token entered"
   chmod go-w ../filebeat/filebeat.yml
   sh docker-compose.sh build backend myapp-client
   sh docker-compose.sh pull elasticsearch kibana filebeat 
   sh docker-compose.sh up --no-build
   unset VAULT_TOKEN
