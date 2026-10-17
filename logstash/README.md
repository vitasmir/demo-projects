#Prace s logy
docker logs --follow --tail=0 myapp-jpa-server-backend-1 |
while IFS= read -r line; do
  printf '%s\n' "$line" |
    tee -a "~/Desktop/demo-projects/filebeat/myapp-log-$(date +%F).json"
done
