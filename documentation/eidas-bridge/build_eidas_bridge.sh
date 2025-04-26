#!/bin/bash

source source_env.sh

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
EIDAS_BRIDGE_REPO="${SCRIPT_DIR}/ssi-eidas-bridge/"

# If not present, clone the repository from Github.
if [ ! -d "$EIDAS_BRIDGE_REPO" ]; then
    git clone https://github.com/validatedid/ssi-eidas-bridge
fi

cd "${EIDAS_BRIDGE_REPO}" || exit 1
git checkout --quiet ${EIDAS_BRIDGE_COMMIT}

docker build -t "gaia-x/data-infrastructure-federation-services/not/notarization-service/ssi-eidas-bridge:${EIDAS_BRIDGE_COMMIT}" .
