#!/usr/bin/env bash

set -e

SCRIPT_HOME="$( cd "$( dirname "$0" )" && pwd )"
CONTAINER_RUNTIME="${CONTAINER_RUNTIME:-docker}"

REPO_URL="https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service"
REGISTRY_URL="registry.gitlab.com"
IMAGE_TAG="${REGISTRY_URL}/gaia-x/data-infrastructure-federation-services/not/notarization-service"

# By default, build container images for those defined services.
TARGET_SERVICES=("ssi-issuance")

# By default, don't push container images.
PUSH=false

# Those variables are necessary when pushing the container image to a registry
REGISTRY_USER=
REGISTRY_TOKEN=

# On a local machine the "git" tool can be used, in the CI pipeline the SHA can be passed via arguments.
COMMIT_SHA=

function handleArguments() {
    while [[ $# -gt 0 ]]
    do
        case $1 in
            --service)
            TARGET_SERVICES=($2)
            shift
            shift
            ;;
            --commit-hash)
            COMMIT_SHA=$2
            shift
            shift
            ;;
            --push)
            PUSH=true
            shift
            ;;
            --user)
            REGISTRY_USER=$2
            shift
            shift
            ;;
            --token)
            REGISTRY_TOKEN=$2
            shift
            shift
            ;;
            *) # unknown option
            shift
            ;;
        esac
    done
}

function createProjectImage() {
  local projectName=$1
  local projectSummary=${projectName}
  local projectVendor="Gaia-X"
  local appVersion=$(jq '.version' -r "${SCRIPT_HOME}/../services/${projectName}/package.json")
  local CONTAINER_TAG="${IMAGE_TAG}/${projectName}:${appVersion}"
  local servicePath="$( cd "${SCRIPT_HOME}/../services/${projectName}" && pwd )"
  local currentDatetime=$(date +%Y-%m-%dT%H:%M:%S%z)

  local labels=(
    --label\ org.opencontainers.image.title="${projectName}"
    --label\ org.opencontainers.image.version="${appVersion}"
    --label\ org.opencontainers.image.description="${projectSummary}"
    --label\ org.opencontainers.image.created="${currentDatetime}"
    --label\ org.opencontainers.image.url="${REPO_URL}"
    --label\ org.opencontainers.image.source="${REPO_URL}"
    --label\ org.opencontainers.image.vendor="${projectVendor}"
    --label\ name="${projectName}"
    --label\ version="${appVersion}"
    --label\ summary="${projectSummary}"
    --label\ description="${projectSummary}"
    --label\ build-date="${currentDatetime}"
    --label\ url="${REPO_URL}"
    --label\ vendor="${projectVendor}"
  )

  if [ -n "${COMMIT_SHA}" ]
  then
    labels+=(--label\ vcs-ref="${COMMIT_SHA}")
    labels+=(--label\ org.opencontainers.image.revision="${COMMIT_SHA}")
  else
    local GIT_HASH=$(git rev-parse --verify HEAD)
    labels+=(--label\ vcs-ref="${GIT_HASH}")
    labels+=(--label\ org.opencontainers.image.revision="${GIT_HASH}")
  fi

  $CONTAINER_RUNTIME build -t "${CONTAINER_TAG}" -f "${servicePath}/Dockerfile" "${servicePath}" ${labels[*]}

  if [ "${PUSH}" == true ]
  then
    $CONTAINER_RUNTIME push "${CONTAINER_TAG}"
  fi
}

handleArguments "$@"

for projectName in ${TARGET_SERVICES[*]}
do
    if [ "${PUSH}" == true ]
    then
      ${CONTAINER_RUNTIME} login ${REGISTRY_URL} -u "${REGISTRY_USER}" -p "${REGISTRY_TOKEN}"
    fi

    createProjectImage "${projectName}"
done
