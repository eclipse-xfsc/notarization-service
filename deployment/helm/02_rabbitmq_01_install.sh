#!/bin/bash

source ./readenv.sh

NOT_API_RABBITMQ_PASSWORD_HASHED=$(docker run -t -i --rm --name rabbitmq  rabbitmq:3-management rabbitmqctl hash_password "$NOT_API_RABBITMQ_USER_PASSWORD" | sed -n '2p' | tr -d '\r')

echo "Username: $NOT_API_RABBITMQ_USER_USERNAME / Hashed Password: $NOT_API_RABBITMQ_PASSWORD_HASHED"

helm upgrade \
  --install rabbitmq \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./rabbitmq/values.ecsec.yaml \
  --set auth.username=$NOT_API_RABBITMQ_USER_USERNAME \
  --set auth.passwordHash=$NOT_API_RABBITMQ_PASSWORD_HASHED \
  --set ingress.hostname=$NOT_API_HOST \
  oci://registry-1.docker.io/bitnamicharts/rabbitmq
