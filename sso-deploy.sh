#!/bin/bash

echo 'Running Keycloak Server'
docker run -d --rm --name sso -p 8888:8080 -p 8443:8443 \
-e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin \
-v "$PWD"/keycloak-resources/keycloak-config.sh:/tmp/keycloak-resources/keycloak-config.sh \
-v "$PWD"/keycloak-resources/thesis-realm.json:/tmp/keycloak-resources/thesis-realm.json \
jboss/keycloak:latest

echo 'Waiting for keycloak to start...'
sleep 15

echo 'Configuring keycloak...'
docker exec -i -t sso /bin/bash -c "chmod +x /tmp/keycloak-resources/keycloak-config.sh && ./tmp/keycloak-resources/keycloak-config.sh"

echo "Done!"
