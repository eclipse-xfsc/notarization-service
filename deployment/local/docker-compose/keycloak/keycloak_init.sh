#!/bin/bash

# SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

echo "Waiting for the keycloak service to be initialised..."


HEALTHCHECK_ENDPOINT="http://${KC_HOSTNAME}:${KEYCLOAK_PORT}/health/ready"
echo "HTTP $HEALTHCHECK_ENDPOINT"

curl --ipv4 --retry 30 --retry-delay 5 --retry-connrefused "$HEALTHCHECK_ENDPOINT"

echo "Starting Provisioing"

cd /opt/keycloak/bin || exit 1

#Authenticate with the Admin Server
./kcadm.sh config credentials --server "http://${KC_HOSTNAME}:${KEYCLOAK_PORT}" --realm master --user "${KEYCLOAK_ADMIN}" --password ${KEYCLOAK_ADMIN_PASSWORD}

#Create Realm Notarization-Realm
./kcadm.sh create realms -s realm=notarization-realm -s enabled=true

#Create Notarizatoin-API Client
./kcadm.sh create clients -r notarization-realm -s clientId=notarization-client -s bearerOnly="true" -s enabled=true -s directAccessGrantsEnabled=true -s clientAuthenticatorType=client-secret -s secret=notarization-api-secret-12345

#Create Profile Client
./kcadm.sh create clients -r notarization-realm -s clientId=profile-client -s bearerOnly="true" -s enabled=true -s directAccessGrantsEnabled=true -s clientAuthenticatorType=client-secret -s secret=not-profile-api-secret-54321

#Create Portal Client
./kcadm.sh create clients -r notarization-realm -s clientId=portal-client -s bearerOnly="false" -s enabled=true -s directAccessGrantsEnabled=true -s implicitFlowEnabled=true -s clientAuthenticatorType=client-secret -s secret=portal-secret-12345 -s 'redirectUris=["http://localhost:8085/login"]'

user_index=1
while [ "$user_index" -le 1 ]
do
    username="notary-0${user_index}"
    # Create notary user

    echo "Creating user ${username}"

    ./kcadm.sh create users -r notarization-realm -s username="${username}" -s enabled=true -s email="${username}@email.com"
    #Set password
    ./kcadm.sh set-password -r notarization-realm --username "${username}" --new-password "${username}-pw"

    # Create role representing a notarization config profile
    profilename="profile-0${user_index}"

    echo "Creating role ${profilename}"
    ./kcadm.sh create roles -r notarization-realm -s name="${profilename}"

    # Give new role and all previous roles to current user
    profile_index=1
    while [ "${profile_index}" -le "${user_index}" ]
    do
        profilename="profile-0${profile_index}"

        echo "Assigning role ${profilename} to ${username}"
        ./kcadm.sh add-roles --uusername "${username}" --rolename "profile-0${profile_index}" -r notarization-realm
        ((profile_index++))
    done

    ((user_index++))
done

# Create Train-Enrollment Client
./kcadm.sh create clients -r notarization-realm -s clientId=train-enrollment-client -s bearerOnly="false" -s enabled=true -s directAccessGrantsEnabled=true -s implicitFlowEnabled=true -s clientAuthenticatorType=client-secret -s secret=train-enrollment-secret-12345

# Create Train-Enrollment User
./kcadm.sh create users -r notarization-realm -s username="train-enrollment" -s enabled=true -s email="train-enrollment@email.com"
./kcadm.sh set-password -r notarization-realm --username "train-enrollment" --new-password "train-enrollment-secret"
./kcadm.sh create roles -r notarization-realm -s name="enrolltf"
./kcadm.sh create roles -r notarization-realm -s name="notary"
./kcadm.sh add-roles --uusername "train-enrollment" --rolename "enrolltf" -r notarization-realm

echo "Completed initialisation of keycloak"
