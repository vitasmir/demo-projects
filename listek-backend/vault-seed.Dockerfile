FROM docker.io/hashicorp/vault:2.0.4

USER root
RUN apk add --no-cache postgresql-client
