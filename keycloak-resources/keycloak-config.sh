#!/bin/bash

# import realm
./opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user admin --password admin
./opt/jboss/keycloak/bin/kcadm.sh create realms -s realm=thesis-realm -s enabled=true
./opt/jboss/keycloak/bin/kcadm.sh create partialImport -r thesis-realm -s ifResourceExists=OVERWRITE -o -f /tmp/keycloak-resources/thesis-realm.json

# Create Administrator User miguel:miguel
./opt/jboss/keycloak/bin/kcadm.sh create users -r thesis-realm -s username=miguel -s enabled=true -s email=m.anciaes@campus.fct.unl.pt -s firstName=Miguel -s lastName=Anciaes -s emailVerified=true
./opt/jboss/keycloak/bin/kcadm.sh set-password -r thesis-realm --username miguel --new-password miguel
./opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername miguel --rolename Administrator -r thesis-realm

# Create BasicUser User bruno:bruno
./opt/jboss/keycloak/bin/kcadm.sh create users -r thesis-realm -s username=reis -s enabled=true -s email=j.reis@campus.fct.unl.pt -s firstName=Joao -s lastName=Reis -s emailVerified=true
./opt/jboss/keycloak/bin/kcadm.sh set-password -r thesis-realm --username reis --new-password reis
./opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername reis --rolename BasicUser -r thesis-realm
