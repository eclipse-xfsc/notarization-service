#!/bin/bash

source ./readenv.sh

helm install postgres --namespace "$NOT_API_NAMESPACE" -f ./postgresql/values.yaml oci://registry-1.docker.io/bitnamicharts/postgresql
