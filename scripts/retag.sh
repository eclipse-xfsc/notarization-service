#!/bin/bash -ex

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

PROJECT_ROOT="${SCRIPT_DIR}/.."

REGISTRY="registry.gitlab.com"
GROUP="gaia-x/data-infrastructure-federation-services/not/notarization-service"

# Services that will be retagged
TARGET_SERVICES=("profile" "request-processing" "oidc-identity-resolver" "scheduler" "revocation")

KIND_NATIVE="native-"

# Ignore native images for now
TARGET_IMAGE_KINDS=("")

# Retag a given service
retag() {
    local project=$1
    local javaVersion=$2
    local kind=$3

    cd "${PROJECT_ROOT}/services/${project}"

    local QUARKUS_VERSION=$(./mvnw help:evaluate -Dexpression=quarkus.platform.version -q -DforceStdout)
    local APP_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

    local CONTAINER_TAG="${APP_VERSION}-quarkus-${QUARKUS_VERSION}-${kind}java${javaVersion}"
    local ADDITIONAL_TAG="${kind}java${javaVersion}-latest"

    if ! command -v crane &> /dev/null
    then
        docker pull $REGISTRY/$GROUP/$project:$CONTAINER_TAG
        docker tag $REGISTRY/$GROUP/$project:$CONTAINER_TAG $REGISTRY/$GROUP/$project:$ADDITIONAL_TAG
        docker push $REGISTRY/$GROUP/$project:$ADDITIONAL_TAG
    else
        crane tag $REGISTRY/$GROUP/$project:$CONTAINER_TAG $ADDITIONAL_TAG
    fi   
}

for project in ${TARGET_SERVICES[*]}
do
    for javaVersion in 17
    do
        for kind in "${TARGET_IMAGE_KINDS[*]}"
        do
            retag $project $javaVersion $kind
        done
    done
done
