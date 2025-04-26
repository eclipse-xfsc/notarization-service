#!/bin/bash -ex

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

# Only build kubernetes resources with jvm images. On request, build also resources with referenced native images.
KIND_NATIVE="native-"
TARGET_IMAGE_KINDS=("")

PROJECT_ROOT="${SCRIPT_DIR}/.."

# By default, build kubernetes resources for those defined services.
TARGET_SERVICES=("oidc-identity-resolver" "profile" "request-processing" "revocation" "scheduler")

DEPLOYMENT_TYPES=("kubernetes")

OUTPUT_DIR=deploy/k8s

handleArguments() {
    while [[ $# -gt 0 ]]
    do
        case $1 in
            --native)
            TARGET_IMAGE_KINDS+=("${KIND_NATIVE}")
            shift
            ;;
            *) # unknown option
            shift
            ;;
        esac
    done
}

create_output_file() {
  local output_file=$1

  if [[ ! -f "$output_file" ]]; then
    echo "Creating output file: $output_file"
    touch $output_file
  fi
}

build_k8s_specs() {
  local project=$1
  local javaVersion=$2
  local kind=$3

  cd "${PROJECT_ROOT}"

  local tag="java${javaVersion}-latest"

  local git_server_url="${GITHUB_SERVER_URL:=https://gitlab.eclipse.org}"
  local git_repo="${GITHUB_REPOSITORY:=eclipse/xfsc/notarization-service/not}"
  local git_ref="${GITHUB_REF_NAME:=main}"

  if [[ "$kind" == "native-" ]]; then
    local mem_limit="128Mi"
    local mem_request="32Mi"
  else
    local mem_limit="512Mi"
    local mem_request="256Mi"
  fi

  echo "Generating app resources for $project/$tag"

  local module_trail=":services:${project}"
 ./gradlew --console=plain ${module_trail}:clean ${module_trail}:assemble \
    -Dquarkus.container-image.tag=$tag \
    -Dquarkus.kubernetes.version=$tag \
    -Dquarkus.kubernetes.resources.limits.memory=$mem_limit \
    -Dquarkus.kubernetes.resources.requests.memory=$mem_request \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-url\"=$git_server_url/$git_repo \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-ref\"=$git_ref
}

process_quarkus_project() {
  local project=$1
  local deployment_type=$2
  local javaVersion=$3
  local kind=$4

  local output_filename="${kind}java${javaVersion}-${deployment_type}"
  local app_generated_input_file="${PROJECT_ROOT}/services/${project}/build/kubernetes/${deployment_type}.yml"
  local project_output_file="${PROJECT_ROOT}/services/${project}/$OUTPUT_DIR/${output_filename}.yml"
  local all_apps_output_file="${PROJECT_ROOT}/$OUTPUT_DIR/base/build/${output_filename}.yml"

  # 1st do the build
  # The build will generate all the resources for the project
  build_k8s_specs $project $javaVersion $kind

  rm -rf $project_output_file

  create_output_file $project_output_file
  create_output_file $all_apps_output_file

  # Now merge the generated resources to the top level (deploy/k8s)
  if [[ -f "$app_generated_input_file" ]]; then
    echo "Copying app generated input ($app_generated_input_file) to $project_output_file and $all_apps_output_file"
    cat $app_generated_input_file >> $project_output_file
    cat $app_generated_input_file >> $all_apps_output_file
  fi
}

handleArguments $@

rm -rf ${PROJECT_ROOT}/$OUTPUT_DIR/base/build/*.yml

for project in ${TARGET_SERVICES[*]}
do
    for javaVersion in 21
    do
        for kind in "${TARGET_IMAGE_KINDS[*]}"
        do
            for deployment_type in "${DEPLOYMENT_TYPES[*]}"
            do
              process_quarkus_project $project $deployment_type $javaVersion $kind
            done
        done
    done
done
