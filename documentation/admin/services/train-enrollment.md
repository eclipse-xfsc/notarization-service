# Train Enrollment

The `train-enrollment` service allows the notary to submit a trust entry to TRAIN, after credentials are issued. This service is integrated as an [extension service](../../developer/extension-services.md).

This service must be configured to access the appropriate TRAIN service.

It is recommended that the submission operation is configured as an action to be executed the notary operator as a [post-issuance-action](../tasks.md).

## OpenAPI

The OpenAPI specification is available [here](../../../services/train-enrollment/deploy/openapi/openapi.yaml).

## Configuration

The following environment variables must be configured:

| Environment Variable                                                | Description                                                                                                                                  | Default   |
| ------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| `GAIA_X_TRAIN_ENROLLMENT_EXTERNAL_URL`                              | The train enrollment url of gaia-x.                                                                                                          |           |
| `QUARKUS_OIDC_CLIENT_AUTH_SERVER_URL`                               | The auth server-url for this service                                                                                                         |           |
| `QUARKUS_OIDC_CLIENT_DISCOVERY_ENABLED`                             | Wether the discovery is enabled                                                                                                              | false     | 
| `QUARKUS_OIDC_CLIENT_TOKEN_PATH`                                    | The oidc token path                                                                                                                          | `/tokens` | 
| `QUARKUS_OIDC_CLIENT_CLIENT_ID`                                     | The oidc client-id                                                                                                                           |           |
| `QUARKUS_OIDC_CLIENT_CREDENTIALS_SECRET`                            | The oidc credential secret                                                                                                                   |           |
| `QUARKUS_OIDC_CLIENT_GRANT_TYPE`                                    | The oidc grant type                                                                                                                          | password  | 
| `QUARKUS_OIDC_CLIENT_GRANT_OPTIONS_PASSWORD_USERNAME`               | The username for the grant type password                                                                                                     |           |
| `QUARKUS_OIDC_CLIENT_GRANT_OPTIONS_PASSWORD_PASSWORD`               | The password for the grant type password                                                                                                     |           |
| `QUARKUS_REST_CLIENT_TRAIN_URL`                                     | Base URL of the train service as defined in [https://gitlab.eclipse.org/eclipse/xfsc/train/](https://gitlab.eclipse.org/eclipse/xfsc/train/) |           |

The following environment variables are optional:

| Environment Variable                         | Description                                | Default   |
| -------------------------------------------- | ------------------------------------------ | --------- |
| `QUARKUS_LOG_LEVEL`                          | The log level.                             | WARN      |
| `QUARKUS_HTTP_HOST`                          | HTTP host to bind to.                      | 8091      |
| `QUARKUS_HTTP_PORT`                          | HTTP port to bind to.                      | localhost |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
