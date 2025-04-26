#!/bin/bash

source ./readenv.sh

echo "Creating/updating secret in namespace $NOT_API_NAMESPACE"

kubectl create secret generic notarization-credentials --namespace "$NOT_API_NAMESPACE" \
    --save-config \
    --dry-run=client \
    "--from-literal=postgresql-admin-username=$NOT_API_DB_ADMIN_USERNAME" \
    "--from-literal=postgresql-admin-password=$NOT_API_DB_ADMIN_PASSWORD" \
    "--from-literal=postgresql-not-api-password=$NOT_API_DB_USER_PASSWORD" \
    "--from-literal=postgresql-not-accept-password=$NOT_API_DB_USER_PASSWORD" \
    "--from-literal=keycloak-admin-password=$NOT_API_KEYCLOAK_ADMIN_PASSWORD" \
    "--from-literal=rabbitmq-password=$NOT_API_RABBITMQ_USER_PASSWORD" \
    "--from-literal=profile-oidc-secret=$NOT_API_PROFILE_OIDC_SECRET" \
    "--from-literal=notarization-oidc-secret=$NOT_API_REQUEST_PROCESSING_OIDC_SECRET" \
    "--from-literal=skid-oidc-secret=$NOT_API_OIDC_IDENTITY_RESOLVER_SKID_SECRET" \
    "--from-literal=auto-notary-decryption-key=$NOT_API_AUTO_NOTARY_DECRYPTION_KEY" \
    "--from-literal=auto-notary-client-secret=$NOT_API_AUTO_NOTARY_OIDC_SECRET" \
    "--from-literal=auto-notary-oidc-password=$NOT_API_AUTO_NOTARY_PASSWORD" \
    -o yaml | \
    kubectl apply --namespace "$NOT_API_NAMESPACE" -f -

echo "To fetch the secret, run the following:"
echo "      kubectl --namespace $NOT_API_NAMESPACE get secret notarization-credentials -o yaml"
