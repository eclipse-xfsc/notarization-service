
#!/usr/bin/env bash

#
# Script to prepare and execute build and push the helm charts.
#
# To run locally, execute the script within a docker container from the root of this repository as such:
#
#    docker run --rm -e DRY_RUN='true' -v .:/running node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/dev-ops/build-executor:main /bin/bash -c '/running/ci/scripts/helm-install/run.sh'
#

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

cd "${SCRIPT_DIR}/../../../"

# WARNING: this is only needed when run within docker!
git config --global --add safe.directory "$(pwd)"

echo "Installing dependencies..."

pip install -r ci/scripts/helm-install/requirements.txt

echo "Begin helm build, helm install and helm deploy script"
python ci/scripts/helm-install/helmBuildPush.py
