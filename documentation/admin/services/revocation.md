# Revocation Service

The `revocation` service manages the revocation of credentials.

## OpenAPI

The OpenAPI specification is available [here](../../../services/revocation/deploy/openapi/openapi.yaml).

## Configuration

The following environment variables must be configured:

| Environment Variable                                | Description                                 | Default   |
| --------------------------------------------------- | ------------------------------------------- | --------- |
| QUARKUS_DATASOURCE_JDBC_URL                         | The datasource URL.                         | `<empty>` |
| QUARKUS_DATASOURCE_USERNAME                         | The datasource username.                    | `<empty>` |
| QUARKUS_DATASOURCE_PASSWORD                         | The datasource password.                    | `<empty>` |
| QUARKUS_REST_CLIENT_SSI_ISSUANCE_API_URL            | URL to the SSI-Issuance service.            | `<empty>` |
| REVOCATION_BASE_URL                                 | The external URL of the revocation service. | `<empty>` |
| QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT | The OTLP endpoint to connect to.            | `<empty>` |

The following environment variables are optional:

| Environment Variable                                    | Description                                                       | Default   |
| ------------------------------------------------------- | ----------------------------------------------------------------- | --------- |
| QUARKUS_HTTP_ACCESS_LOG_ENABLED                         | If access logging is enabled.                                     | false     |
| QUARKUS_LOG_LEVEL                                       | The default log level.                                            | INFO      |
| QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PEM_CERTS | Comma-separated list of the trust certificate files (Pem format). | `<empty>` |
| QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE                    | The datasource pool maximum size.                                 | `<empty>` |


**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
