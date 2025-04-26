#!/usr/bin/env bash

set -e

export COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-notarization-service}"

SCRIPT_HOME="$( cd "$( dirname "$0" )" && pwd )"

# Relative to this script's location
COMPOSE_FILES_DIR="../deploy/local/docker-compose"

LEDGER_COMPOSE_FILES=(
  "ledger.yml"
)

LEDGER_NETWORK_NAME="${LEDGER_NETWORK_NAME:-ledger_network}"
WAIT_FOR_LEDGER="${WAIT_FOR_LEDGER:-1}"
LEDGER_TIMEOUT="${LEDGER_TIMEOUT:-60}"
EXTRA_ARGS=()
LOCAL_SSI=


docker="docker --log-level error "
dockerCompose="$docker compose"

# Resolve the absolute path to docker compose files
composeFilesDirectory=$(cd "$SCRIPT_HOME" && readlink -f $COMPOSE_FILES_DIR)

function handleArguments() {
  for arg in "$@"; do
    shift
    case "$arg" in
      --build)
        EXTRA_ARGS+=("--build")
        ;;
      --clean)
        EXTRA_ARGS+=("--remove-orphans" "-v")
        ;;
      --recreate)
        EXTRA_ARGS+=("--force-recreate")
        ;;
      --debug-acapy)
        export ENABLE_PTVSD=1
        ;;
      --debug-acapy-pycharm)
        export ENABLE_PYDEVD_PYCHARM=1
        ;;
      *)
        set -- "$@" "@arg"
        ;;
    esac
  done
}

function getLedgerComposeFiles() {
  local ledgerComposeFiles=()
  for composeFile in "${LEDGER_COMPOSE_FILES[@]}"; do
    ledgerComposeFiles+=("-f $(cd "$SCRIPT_HOME" && readlink -f "$composeFilesDirectory/$composeFile")")
  done
  echo "${ledgerComposeFiles[*]}"
}

function usage () {
  cat <<-EOF
  Usage $0 [command]

  Commands:
  - start-ledger
  - stop-ledger
  - cli
EOF
  exit 1
}

function pingLedger (){
  ledger_url=${1}

  # ping ledger web browser for genesis txns
  local rtnCd=$(curl -s --write-out '%{http_code}' --output /dev/null ${ledger_url}/status/text)
  if (( ${rtnCd} == 200 )); then
    return 0
  else
    return 1
  fi
}

function waitForLedger () {
  (
    # if flag is set, wait for ledger to activate before continuing
    local rtnCd=0
    if [ ! -z "${WAIT_FOR_LEDGER}" ]; then
      # Wait for ledger server to start ...
      local startTime=${SECONDS}
      # use global LEDGER_URL
      local LEDGER_URL="http://localhost:9000"
      printf "waiting for ledger to start"
      while ! pingLedger "$LEDGER_URL"; do
        printf "."
        local duration=$(($SECONDS - $startTime))
        if (( ${duration} >= ${LEDGER_TIMEOUT} )); then
          echo "\nThe Indy Ledger failed to start within ${duration} seconds.\n"
          rtnCd=1
          break
        fi
        sleep 1
      done
    fi
    return ${rtnCd}
  )
}

function startLedger () {
  handleArguments "$@"

  echo "Creating network ""$LEDGER_NETWORK_NAME"""
  local networkCreateResult=$(${docker} network create "$LEDGER_NETWORK_NAME" --subnet "10.0.0.0/24")
  if [[ $? -ne 0 ]]; then
    echo "$networkCreateResult"
  fi

  ${dockerCompose} $(getLedgerComposeFiles) up --detach ${EXTRA_ARGS[*]} ledger-nodes ledger-browser
  waitForLedger
  startClient ./cli-scripts/register-user-did
}

function stopLedger () {
  handleArguments "$@"
  ${dockerCompose} $(getLedgerComposeFiles) down -v ${EXTRA_ARGS[*]}

  echo "Removing network ""$LEDGER_NETWORK_NAME"""
  local networkRemoveResult=$(${docker} network rm "$LEDGER_NETWORK_NAME")
  if [[ $? -ne 0 ]]; then
    echo "$networkRemoveResult"
  fi
}

function startClient () {
  handleArguments "$@"
  args=$@
  if [[ " ${EXTRA_ARGS[*]} " == *" --build "* ]]; then
    args=("${args[@]/--build}")
    ${dockerCompose} $(getLedgerComposeFiles) build ledger-client
  fi
  ${dockerCompose} $(getLedgerComposeFiles) run -it --rm ledger-client ./scripts/manage start-client ${args[*]}
}

function showLogs () {
  handleArguments "$@"
  ${dockerCompose} $(getComposeFiles) $(getLedgerComposeFiles) logs "$@"
}

pushd "${SCRIPT_HOME}" >/dev/null
COMMAND=$1
shift || COMMAND=usage

case "${COMMAND}" in
  start-ledger)
    startLedger "$@"
    ;;
  stop-ledger)
    stopLedger "$@"
    ;;
  cli)
    startClient "$@"
    ;;
  logs)
    showLogs "$@"
    ;;
  *)
    usage
    ;;
esac

popd >/dev/null
