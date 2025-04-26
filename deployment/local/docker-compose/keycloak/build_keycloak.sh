#!/bin/bash

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

docker build -f "${SCRIPT_DIR}/Dockerfile.keycloak" -t node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/not/keycloak-jq:23.0.7 .
