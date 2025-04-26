#!/bin/bash

source source_env.sh

if [ ! -f "payload.json" ]; then
    echo -e "File 'payload.json' is missing. Copy or move 'payload.example.json' as reference."
    exit 1
fi

generate_post_data() {
    local PAYLOAD
    PAYLOAD=$(cat payload.json)
    cat <<EOF
{
  "issuer": "${DID_ISSUER}",
  "password": "${PKCS12_PASSWORD}",
  "payload": ${PAYLOAD}
}
EOF
}

if ! curl -s --location --request POST 'http://127.0.0.1:9002/eidas-bridge/v1/signatures' \
    --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    --data "$(generate_post_data)"; then
    echo "Error: Cannot create signature. Make sure eIDAS Bridge and Redis are running."
fi
