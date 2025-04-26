#!/bin/bash -e

SCRIPTS_DIR=$(dirname "$0")

FORCE_NEW_DID=

handleArguments() {
  while [[ $# -gt 0 ]]
  do
    case $1 in
      --force-new-did)
      FORCE_NEW_DID=true
      shift
      ;;
      *) # unknown option
      shift
      ;;
    esac
  done
}

handleArguments "$@"

args=()
if [ "$FORCE_NEW_DID" == true ]; then
  args+=( '--force-new' )
fi
HOLDER_DID=$("${SCRIPTS_DIR}/create-holder-did.sh" "${args[@]}")

INVITATION_URL=$("${SCRIPTS_DIR}/create-holder-invitation.sh")
ISSUANCE_TIMESTAMP=$(date +%Y-%m-%dT%H:%M:%S%z)

cat << EOF
{
  "profileID": "demo-vc-issuance-01-simple",
  "holderDID": "${HOLDER_DID}",
  "credentialData": {
    "credentialSubject": {},
    "evidence": []
  },
  "issuanceTimestamp": "${ISSUANCE_TIMESTAMP}",
  "invitationURL": "${INVITATION_URL}",
  "successURL": "http://request-processing:8084/success",
  "failureURL": "http://request-processing:8084/failure"
}
EOF
