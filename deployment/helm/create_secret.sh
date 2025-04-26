#!/bin/bash

kubectl create secret generic the-secret-name -n not-accept \
    --save-config \
    --dry-run=client \
    --from-literal=postgresql-admin-username=not_api \
    --from-literal=postgresql-admin-password=5b3506e5-c164-4400-aaec-54a88d87f00a \
    --from-literal=postgresql-not-api-password=5b3506e5-c164-4400-aaec-54a88d87f00a \
    --from-literal=postgresql-not-accept-password=5b3506e5-c164-4400-aaec-54a88d87f00a \
    --from-literal=keycloak-admin-password=kc-not-accept-admin \
    --from-literal=rabbitmq-password=not-accept-rabbitmq \
    --from-literal=KEY=VALUE \
    -o yaml | \
    kubectl apply -f -
