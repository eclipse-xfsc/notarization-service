# Request-Processing Service

The `request-processing` services provides endpoints for the business owner to submit notarization requests and the notarization operator to manage such requests.
The `request-processing` service uses OAuth2 for the request management endpoints to ensure that a caller is authorized.
The following environment variables must be configured:

## OpenAPI

The OpenAPI specification is available [here](../../../services/request-processing/deploy/openapi/openapi.yaml).

## Configuration

| Environment Variable                                        | Description                                                                                            | Default                 |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ | ----------------------- |
| QUARKUS_DATASOURCE_REACTIVE_URL                             | The datasource URL.                                                                                    | `<empty>`               |
| QUARKUS_DATASOURCE_USERNAME                                 | The datasource username.                                                                               | `<empty>`               |
| QUARKUS_DATASOURCE_PASSWORD                                 | The datasource password.                                                                               | `<empty>`               |
| QUARKUS_REST_CLIENT_PROFILE_API_URL                         | The URL to the profile service.                                                                        | <http://localhost:9196> |
| QUARKUS_OIDC_AUTH_SERVER_URL                                | The base URL of the OpenID Connect (OIDC) server.                                                      | `<empty>`               |
| QUARKUS_OIDC_CLIENT_ID                                      | The OAuth2 client id.                                                                                  | `<empty>`               |
| QUARKUS_OIDC_CREDENTIALS_SECRET                             | The OAuth2 client secret.                                                                              | `<empty>`               |
| AMQP_HOST                                               | AMQP Host.                                                                                         | `<empty>`               |
| AMQP_PORT                                               | AMQP Port.                                                                                         | `<empty>`               |
| AMQP_USERNAME                                           | AMQP username.                                                                                     | `<empty>`               |
| AMQP_PASSWORD                                           | AMQP password.                                                                                     | `<empty>`               |
| QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT         | The OTLP endpoint to connect to.                                                                       | `<empty>`               |
| QUARKUS_REST_CLIENT_DSS_API_URL                             | REST API URL of the DSS service.                                                                       | `<empty>`               |
| NOTARIZATION_PROCESSING_TERMINATED_SESSION_RETENTION_PERIOD | Terminated and issued sessions will be deleted after this duration.                                    | P1Y                     |
| NOTARIZATION_PROCESSING_SESSION_TIMEOUT_PERIOD              | Sessions requiring a requestor interactions (like identification) will be deleted after this duration. | P1W                     |
| NOTARIZATION_PROCESSING_SESSION_SUBMISSION_TIMEOUT_PERIOD   | Sessions without submission data will be deleted after this duration.                                  | P1D                     |
| NOTARIZATION_PROCESSING_HTTP_AUDIT_LOGS_RETENTION_PERIOD    | HTTP audit logs will be deleted after this duration.                                                   | P6Y                     |

For periods, the standard `java.time.Period` format is used. You can learn more about it in the [Period#parse() javadoc](https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-).

If AMQP is used over TLS, just set some default values for `AMQP_USERNAME` and `AMQP_PASSWORD`. The environment variables for enabling TLS are shown below.

The following environment variables are optional:

| Environment Variable                                    | Description                                                                                                                                   | Default   |
| ------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| NOTARIZATION_AMQP_SSL                               | If TLS is enabled for AMQP communication.                                                                                                 | false     |
| NOTARIZATION_AMQP_CA_PATH                           | Path to the CA ceritifcate (PEM).                                                                                                             | `<empty>` |
| NOTARIZATION_AMQP_TLS_CERT_PATH                     | Path to the TLS certificate (PEM).                                                                                                            | `<empty>` |
| NOTARIZATION_AMQP_TLS_KEY_PATH                      | Path to the TLS key (PEM).                                                                                                                    | `<empty>` |
| QUARKUS_HTTP_ACCESS_LOG_ENABLED                         | If access logging is enabled.                                                                                                                 | false     |
| QUARKUS_LOG_LEVEL                                       | The default log level.                                                                                                                        | INFO      |
| QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PEM_CERTS | Comma-separated list of the trust certificate files (Pem format).                                                                             | `<empty>` |
| QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE                    | The datasource pool maximum size.                                                                                                             | `<empty>` |
| QUARKUS_OIDC_DISCOVERY_ENABLED                          | Disable evaluating the OIDC discovery document. Needed if INTROSPECTION_PATH should be used without fetching a JWKs set for local validation. | `<empty>` |
| QUARKUS_OIDC_INTROSPECTION_PATH                         | Set the token introspection endpoint for validating access tokens.                                                                            | `<empty>` |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` or `AMQP_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config

When using AMQP over TLS, the required CA certificate, TLS certificate and key must be mounted into the `request-processing` pod and the path configured in the respective environment variable.

