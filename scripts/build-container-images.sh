#!/bin/bash -ex

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

KIND_NATIVE="native-"

# Only build native images on request, and not during CI pipeline.
TARGET_IMAGE_KINDS=("")

# Memory available for use when building native images
NATIVE_IMAGE_XMX=${NATIVE_IMAGE_XMX:-6G}

PROJECT_ROOT="${SCRIPT_DIR}/.."

REPO_URL="https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service"

# Those variables are necessary when pushing the container image to a registry
REGISTRY_USER=
REGISTRY_TOKEN=

# By default, build container images for those defined services.
TARGET_SERVICES=("profile" "request-processing" "oidc-identity-resolver" "scheduler" "revocation")

# By default, don't push container images.
PUSH=false

# On a local machine the "git" tool can be used, in the CI pipeline the SHA can be passed via arguments.
COMMIT_SHA=

handleArguments() {
    while [[ $# -gt 0 ]]
    do
        case $1 in
            --service)
            TARGET_SERVICES=($2)
            shift
            shift
            ;;
            --native)
            TARGET_IMAGE_KINDS+=("${KIND_NATIVE}")
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

createLabelDateTime() {
    # https://unix.stackexchange.com/questions/120484/what-is-a-standard-command-for-printing-a-date-in-rfc-3339-format
    # 'date' implements RFC 3339 without the 'T' seperating date and time!
    echo `date +%Y-%m-%dT%H:%M:%S%z`

    return $?
}

# Inspect the tooling to determine which build image is appropriate
determineNativeBuilder() {
    if [ -e "${GRAALVM_HOME}/GRAALVM-README.md" ]
    then
        local BUILD_TOOL="graalvm"
    elif [ -e "${GRAALVM_HOME}/README.md" ]
    then
        if [[ `head "${GRAALVM_HOME}"/README.md -n 1 | grep -c Mandrel` -ge 1 ]];
        then
            local BUILD_TOOL="mandrel"
        else
            local BUILD_TOOL="graalvm"
        fi
    else
        # Sane fallback
        local BUILD_TOOL="graalvm"
    fi

    if [[ "${BUILD_TOOL}" == "mandrel" ]];
    then
        echo "quay.io/quarkus/ubi-quarkus-mandrel:21.3-java"
    else
        echo "quay.io/quarkus/ubi-quarkus-native-image:22.0-java"
    fi
}

# create_project_container(project, javaVersion, kind)
#
# Build a container given a quarkus project.
create_project_container() {
    local project=$1
    local javaVersion=$2
    local kind=$3
    local CURRENT_DATETIME=`createLabelDateTime`
    local SUMMARY="The microservice '${project}', part of the 'Notarization API' in the distributed GAIA-X ecosystem."

    cd "${PROJECT_ROOT}/services/${project}"

    local QUARKUS_VERSION=$(./mvnw help:evaluate -Dexpression=quarkus.platform.version -q -DforceStdout)
    local APP_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

    local CONTAINER_TAG="${APP_VERSION}-quarkus-${QUARKUS_VERSION}-${kind}java${javaVersion}"
    # Maybe consider in future retagging, currently using additional tags with JIB and Gitlab Registry does not work
    local ADDITIONAL_TAG="${kind}java${javaVersion}-latest"

    if [[ "${kind}" == "${KIND_NATIVE}" ]];
    then
        local NATIVE_BUILDER=`determineNativeBuilder`

        local BASE_IMAGE="quay.io/quarkus/ubi-quarkus-native-image:22.0-java${javaVersion}"
        local EXTRA_ARGS=("-Pnative" \
            "-Dquarkus.native.container-build=true" \
            "-Dquarkus.native.reuse-existing=true" \
            -Dquarkus.native.native-image-xmx=${NATIVE_IMAGE_XMX} \
            "-Dquarkus.native.builder-image=${NATIVE_BUILDER}${javaVersion}" \
            "-Dquarkus.jib.base-native-image=${BASE_IMAGE}" )
    else
        local BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-${javaVersion}-runtime:1.11"
        local EXTRA_ARGS=()
    fi
    if [ -z "${CI}" ]
    then
        EXTRA_ARGS+=("-Dquarkus.container-image.additional-tags=${ADDITIONAL_TAG}")
    fi

    # Annotation specification: https://github.com/opencontainers/image-spec/blob/main/annotations.md
    local LABEL_PREFIX="-Dquarkus.jib.labels"

    [[ ! -z "$REGISTRY_USER" ]] && EXTRA_ARGS+=("-Dquarkus.container-image.username=${REGISTRY_USER}")
    [[ ! -z "$REGISTRY_TOKEN" ]] && EXTRA_ARGS+=("-Dquarkus.container-image.password=${REGISTRY_TOKEN}")

    if [ ! -z "$COMMIT_SHA" ]
    then
        EXTRA_ARGS+=("${LABEL_PREFIX}.\"vcs-ref\"=${COMMIT_SHA}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.revision\"=${COMMIT_SHA}")
    else
        local GIT_HASH=`git rev-parse --verify HEAD`
        EXTRA_ARGS+=("${LABEL_PREFIX}.\"vcs-ref\"=${GIT_HASH}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.revision\"=${GIT_HASH}")
    fi

    ./mvnw -B clean package -DskipTests \
            -Dmaven.compiler.release="${javaVersion}" \
            -Dquarkus.http.host=0.0.0.0 \
            -Dquarkus.container-image.build=true \
            -Dquarkus.container-image.push=$PUSH \
            "-Dquarkus.container-image.tag=${CONTAINER_TAG}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.created\"=${CURRENT_DATETIME}" \
            "${LABEL_PREFIX}.\"build-date\"=${CURRENT_DATETIME}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.title\"=${project}" \
            "${LABEL_PREFIX}.name=${project}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.description\"=${SUMMARY}" \
            "${LABEL_PREFIX}.summary=${SUMMARY}\"" \
            "${LABEL_PREFIX}.description=${SUMMARY}\"" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.version\"=${APP_VERSION}" \
            "${LABEL_PREFIX}.\"version\"=${APP_VERSION}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.vendor\"=Gaia-X" \
            "${LABEL_PREFIX}.\"vendor\"=Gaia-X" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.url\"=${REPO_URL}" \
            "${LABEL_PREFIX}.\"url\"=${REPO_URL}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.source\"=${REPO_URL}" \
            "${LABEL_PREFIX}.\"org.opencontainers.image.base.name\"=${BASE_IMAGE}" \
            -e \
            ${EXTRA_ARGS[*]}
}

handleArguments $@

for project in ${TARGET_SERVICES[*]}
do
    for javaVersion in 17
    do
        for kind in "${TARGET_IMAGE_KINDS[*]}"
        do
            create_project_container $project $javaVersion $kind

        done
    done
done
