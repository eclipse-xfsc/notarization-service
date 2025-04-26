#!/bin/bash

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

docker build -f "${SCRIPT_DIR}/Dockerfile.rabbitmq" -t node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/not/rabbitmq-dev:3.13.7-management .
