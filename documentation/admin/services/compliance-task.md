# Compliance Task

The `compliance-task` service allows requestors to provide data which are use with the [Compliance Service - https://gitlab.com/gaia-x/lab/compliance/gx-compliance ](https://gitlab.com/gaia-x/lab/compliance/gx-compliance) and integrated in the issuance process.

## OpenAPI

The OpenAPI specification is available [here](../../../services/compliance-task/deploy/openapi/openapi.yaml).

## Configuration

The following environment variables must be configured:

| Environment Variable                   | Description                                                                                                                                                                              | Default   |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| `GAIA_X_COMPLIANCE_TASK_EXTERNAL_URL`  | The external URL of the service                                                                                                                                                          |           |
| `QUARKUS_REST_CLIENT_COMPLIANCE_URL`   | The base URL of the compliance service as defined in [https://gaia-x.gitlab.io/policy-rules-committee/trust-framework/](https://gaia-x.gitlab.io/policy-rules-committee/trust-framework/)|           |

The following environment variables are optional:

| Environment Variable                         | Description                                | Default   |
| -------------------------------------------- | ------------------------------------------ | --------- |
| `QUARKUS_LOG_LEVEL`                          | The log level.                             | WARN      |
| `QUARKUS_HTTP_HOST`                          | HTTP host to bind to.                      | 8090      |
| `QUARKUS_HTTP_PORT`                          | HTTP port to bind to.                      | localhost |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
