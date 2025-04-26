#!/bin/bash

source ./readenv.sh

helm upgrade --install profile \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./profile/values.ecsec.yaml \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/profile \
  --set config.datasourceReactiveUrl=postgresql://$NOT_API_DB_HOST:5432/profile \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.oidcAuthServerUrl=$NOT_API_KEYCLOAK_BASE_URL/realms/$NOT_API_KEYCLOAK_REALM \
  --set config.oidcIntrospectionPath=$NOT_API_KEYCLOAK_BASE_URL/realms/$NOT_API_KEYCLOAK_REALM/protocol/openid-connect/token/introspect \
  --set config.oidcClientId=$NOT_API_PROFILE_OIDC_CLIENT \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.restClientRevocationApiUrl=https://$NOT_API_HOST/revocation \
  --set config.restClientSsiIssuanceV1ApiUrl=http://ssi-issuance.$NOT_API_INTERNAL_HOST:8080 \
  --set config.restClientSsiIssuanceV2ApiUrl=http://ssi-issuance2.$NOT_API_INTERNAL_HOST:8089 \
  ./profile

helm upgrade --install request-processing \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./request-processing/values.ecsec.yaml \
  --set envConfig.AMQP_USERNAME=$NOT_API_RABBITMQ_USER_USERNAME \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/request_processing \
  --set config.datasourceReactiveUrl=postgresql://$NOT_API_DB_HOST:5432/request_processing \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.notarizationProcessingInternalUrl=https://$NOT_API_HOST/request-processing \
  --set config.oidcAuthServerUrl=$NOT_API_KEYCLOAK_BASE_URL/realms/$NOT_API_KEYCLOAK_REALM \
  --set config.oidcClientId=$NOT_API_REQUEST_PROCESSING_OIDC_CLIENT \
  --set config.oidcIntrospectionPath=$NOT_API_KEYCLOAK_BASE_URL/realms/$NOT_API_KEYCLOAK_REALM/protocol/openid-connect/token/introspect \
  --set config.amqpHost=rabbitmq.$NOT_API_INTERNAL_HOST \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.restClientDssApiUrl=http://dss.$NOT_API_INTERNAL_HOST:8080/services/rest \
  --set config.restClientRevocationApiUrl=https://$NOT_API_HOST/revocation \
  --set config.restClientProfileApiUrl=https://$NOT_API_HOST/profile \
  --set config.restClientSsiIssuanceV2ApiUrl=http://ssi-issuance2.$NOT_API_INTERNAL_HOST:8089 \
  --set config.browserIdentificationUrl=https://$NOT_API_HOST/oidc-identity-resolver/session/ \
  --set config.vcIdentificationUrl=http://ssi-issuance.$NOT_API_INTERNAL_HOST:8088/credential/verify \
  --set config.mpMessagingOutgoingRequestorAddress=$NOT_API_RABBITMQ_NOTIFICATION_REQUESTOR \
  --set config.mpMessagingOutgoingOperatorAddress=$NOT_API_RABBITMQ_NOTIFICATION_OPERATOR \
  --set config.gaiaxTasksOidcIdentifyLocation=https://$NOT_API_HOST/oidc-identity-resolver/session \
  --set config.gaiaxTasksComplianceLocation=https://$NOT_API_HOST/compliance-task \
  ./request-processing

helm upgrade --install ssi-issuance2 \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./ssi-issuance2/values.ecsec.yaml \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/ssi_issuance2 \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.issuance2ServiceUrl=http://ssi-issuance2.$NOT_API_INTERNAL_HOST:8089 \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.restClientRevocationApiUrl=https://$NOT_API_HOST/revocation \
  --set config.restClientProfileApiUrl=https://$NOT_API_HOST/profile \
  --set config.restClientOfferApiUrl=https://$NOT_API_HOST/oid4vci \
  --set config.restClientAcapyApiUrl=https://$NOT_API_HOST/acapy \
  ./ssi-issuance2

helm upgrade --install scheduler \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./scheduler/values.ecsec.yaml \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/scheduler \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.restClientRevocationApiUrl=https://$NOT_API_HOST/revocation \
  --set config.restClientProfileApiUrl=https://$NOT_API_HOST/profile \
  --set config.restClientRequestProcessingApiUrl=https://$NOT_API_HOST/request-processing \
  ./scheduler

helm upgrade --install oid4vci \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./oid4vci/values.ecsec.yaml \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/oid4vci \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.gaiaXOid4vciIssuerUrl=https://$NOT_API_HOST/oid4vci \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.restClientProfileApiUrl=https://$NOT_API_HOST/profile \
  ./oid4vci

helm upgrade --install oidc-identity-resolver \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./oidc-identity-resolver/values.ecsec.yaml \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/oidc_identity_resolver \
  --set config.datasourceReactiveUrl=postgresql://$NOT_API_DB_HOST:5432/oidc_identity_resolver \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.oidcAuthServerUrl=$NOT_API_KEYCLOAK_BASE_URL/realms/$NOT_API_KEYCLOAK_REALM \
  --set config.oidcClientId=$NOT_API_OIDC_IDENTITY_RESOLVER_SKID_CLIENT \
  --set config.oidcIdentityResolverExternalUrl=https://$NOT_API_HOST/oidc-identity-resolver \
  --set config.redirectLoginFailureUrl=$NOT_API_OIDC_IDENTITY_REDIRECT_FAILURE_URL \
  --set config.redirectLoginSuccessUrl=$NOT_API_OIDC_IDENTITY_REDIRECT_SUCCESS_URL \
  ./oidc-identity-resolver

helm upgrade --install revocation \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./revocation/values.ecsec.yaml \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.datasourceJdbcUrl=jdbc:postgresql://$NOT_API_DB_HOST:5432/revocation \
  --set config.datasourceUsername=$NOT_API_DB_USER_USERNAME \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.restClientRevocationApiUrl=https://$NOT_API_HOST/revocation \
  --set config.revocationBaseUrl=https://$NOT_API_HOST/revocation \
  --set config.revocationMinIssueInterval=PT0S \
  ./revocation

helm upgrade --install auto-notary \
  --namespace "$NOT_API_NAMESPACE" \
  -f ./auto-notary/values.ecsec.yaml \
  --set ingress.hosts[0].host=$NOT_API_HOST \
  --set ingress.tls[0].hosts[0]=$NOT_API_HOST \
  --set config.notarizationProcessingInternalUrl=https://$NOT_API_HOST/request-processing \
  --set config.otelConnectorUrl=http://otel-collector.$NOT_API_INTERNAL_HOST:4317 \
  --set config.oidcClientId=$NOT_API_AUTO_NOTARY_OIDC_CLIENT \
  --set config.oidcAuthServerUrl=$NOT_API_KEYCLOAK_BASE_URL/realms/$NOT_API_KEYCLOAK_REALM \
  --set config.oidcUsername=$NOT_API_AUTO_NOTARY_USERNAME \
  ./auto-notary
