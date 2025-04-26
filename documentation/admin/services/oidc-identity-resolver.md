# OIDC-Identity-Resolver Service

The idea of the `oidc-identity-resolver` service is to connect to other IdP systems via OIDC, for example an eID system.
This service can be used in the notarization system to identify requestors.
The following environment variables must be configured:

## OpenAPI

The OpenAPI specification is available [here](../../../services/oidc-identity-resolver/deploy/openapi/openapi.yaml).

## Configuration

| Environment Variable            | Description                                       | Default   |
|---------------------------------|---------------------------------------------------|-----------|
| QUARKUS_OIDC_AUTH_SERVER_URL    | The base URL of the OpenID Connect (OIDC) server. | `<empty>` |
| QUARKUS_OIDC_CLIENT_ID          | Identity Provider OIDC client id.                 | `<empty>` |
| QUARKUS_OIDC_CREDENTIALS_SECRET | Identity Provider OIDC client secret.             | `<empty>` |
| DEMO_IDENTITY_OIDC_EXTERNAL_URL | The external URL of the Identity-OIDC service.    | `<empty>` |
| QUARKUS_DATASOURCE_REACTIVE_URL | The datasource URL.                               | `<empty>` |
| QUARKUS_DATASOURCE_USERNAME     | The datasource username.                          | `<empty>` |
| QUARKUS_DATASOURCE_PASSWORD     | The datasource password.                          | `<empty>` |

Some optional environment variables that can be used:

| Environment Variable                                    | Description                                                                                                                                   | Default   |
|---------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| QUARKUS_HTTP_ACCESS_LOG_ENABLED                         | If access logging is enabled.                                                                                                                 | false     |
| QUARKUS_LOG_LEVEL                                       | The default log level.                                                                                                                        | INFO      |
| QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PEM_CERTS | Comma-separated list of the trust certificate files (Pem format).                                                                             | `<empty>` |
| QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE                    | The datasource pool maximum size.                                                                                                             | `<empty>` |
| QUARKUS_OIDC_DISCOVERY_ENABLED                          | Disable evaluating the OIDC discovery document. Needed if INTROSPECTION_PATH should be used without fetching a JWKs set for local validation. | `<empty>` |
| QUARKUS_OIDC_INTROSPECTION_PATH                         | Set the token introspection endpoint for validating access tokens.                                                                            | `<empty>` |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
