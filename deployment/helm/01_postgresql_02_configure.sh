#!/bin/bash

source ./readenv.sh


source ./readenv.sh

echo "Given variable namespace is: $NOT_API_NAMESPACE"

# echo "Printing variables with Prefix 'NOT_API':"
# printenv | grep NOT_API

execute_script() {

    kubectl exec statefulsets/postgres-postgresql \
        --namespace $NOT_API_NAMESPACE -i -t -- \
        /opt/bitnami/scripts/postgresql/entrypoint.sh bash -c "PGPASSWORD=$NOT_API_DB_ADMIN_PASSWORD psql $*"
}

create_user() {
    echo "Creating user $NOT_API_DB_USER_USERNAME"
    execute_script "-d postgres -c \"CREATE user $NOT_API_DB_USER_USERNAME WITH password '$NOT_API_DB_USER_PASSWORD';\""
}

create_database() {
    local DATABASE=$1
    echo "Creating database $DATABASE and giving permissions to $NOT_API_DB_USER_USERNAME"
    execute_script "-d postgres -c \"create database $DATABASE;\" -c \"grant all privileges on database $DATABASE to $NOT_API_DB_USER_USERNAME;\""
    echo "Granting schema access to $NOT_API_DB_USER_USERNAME"
    execute_script "-d $DATABASE -c \"grant all on schema public to $NOT_API_DB_USER_USERNAME;\""
}

create_user
create_database not_keycloak
create_database compliance_task
create_database oid4vci
create_database oidc_identity_resolver
create_database profile
create_database request_processing
create_database revocation
create_database scheduler
create_database ssi_issuance2
