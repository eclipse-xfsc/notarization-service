#!/bin/bash

source source_env.sh

usage() {
    cat <<EOF
Usage: ${BASH_SOURCE[0]}

Make sure you have adjusted the values in the .env files. Copy or move '.env.example' as reference.

EOF
}

if [ ! -f "${P12_FILE}" ]; then
    echo -e "File '${P12_FILE}' does not exist.\n"
    usage
    exit 1
fi

if [[ -z ${DID_IDENTIFIER} || -z ${DID_ISSUER} ]]; then
    echo -e "At least one DID is missing.\n"
    usage
    exit 1
fi

# Convert p12 file to hex:
P12_FILE_ENCODED=$(xxd -p -c 1000000 <"${P12_FILE}")

# Use simple string/replace instead of full url encoding.
DID_IDENTIFIER_ENCODED="${DID_IDENTIFIER//:/%3A}"

generate_put_data() {
    cat <<EOF
{
  "did": "${DID_ISSUER}",
  "eidasQec": "${P12_FILE_ENCODED}"
}
EOF
}

if curl -s -o /dev/null --location --request PUT "http://localhost:9002/eidas-bridge/v1/eidas-keys/${DID_IDENTIFIER_ENCODED}" \
    --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    --data "$(generate_put_data)"; then
    echo "Successfully saved DID in the data store."
else
    echo "Error: Cannot save DID in the data store. Make sure eIDAS Bridge and Redis are running."
fi
