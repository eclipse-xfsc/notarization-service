#!/bin/bash -e

ACAPY_BASE_URL="${ACAPY_BASE_URL:-http://localhost:30080}"

DID=$(curl -s -X 'GET' "${ACAPY_BASE_URL}/wallet/did?key_type=ed25519&method=key" | jq -r '.results[0].did')
FORCE_NEW=

handleArguments() {
  while [[ $# -gt 0 ]]
  do
    case $1 in
      --force-new)
      FORCE_NEW=true
      shift
      ;;
      *) # unknown option
      shift
      ;;
    esac
  done
}

handleArguments "$@"

if [ "$FORCE_NEW" == true ] || [ "$DID" == "null" ]; then
  curl -s -X 'POST' \
    "${ACAPY_BASE_URL}/wallet/did/create" \
    -H 'accept: application/json' \
    -H 'Content-Type: application/json' \
    -d '{
    "method": "key",
    "options": {
      "key_type": "ed25519"
    }
  }' | jq -r '.result.did'
else
  echo "$DID"
fi
