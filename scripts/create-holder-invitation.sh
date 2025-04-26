#!/bin/bash -e

ACAPY_BASE_URL="${ACAPY_BASE_URL:-http://localhost:30080}"

curl -s -X 'POST' \
  "${ACAPY_BASE_URL}/connections/create-invitation?auto_accept=true&multi_use=true" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{}' | jq -r '.invitation_url'
