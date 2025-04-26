#!/bin/bash

source ./readenv.sh

helm upgrade --install keycloak --namespace "$NOT_API_NAMESPACE" \
  -f ./keycloak/values.ecsec.yaml \
  --set ingress.hostname=$NOT_API_KEYCLOAK_HOST \
  --set externalDatabase.host=$NOT_API_DB_HOST \
  oci://registry-1.docker.io/bitnamicharts/keycloak
