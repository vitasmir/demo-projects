 #!/bin/bash
    
    cd myapp-jpa-server
   chmod go-w ../filebeat/filebeat.yml
   sh docker-compose-without-vault.sh up --build

