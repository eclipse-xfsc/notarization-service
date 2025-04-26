#!/bin/bash

source ./readenv.sh

helm upgrade --install dss \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./dss/values.yaml \
  --set image.repository=public.docker.ecsec.de/not-api/dss:latest \
  --set replicaCount=1 \
  oci://node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/dev-ops/dss-demo-webapp
