# OID4VP-task service

The `oid4vp-task` service allows the creation of verifiable presentations as defined in [`OpenID for Verifiable Presentations - draft 20` https://openid.net/specs/openid-4-verifiable-presentations-1_0.html](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html).

## OpenAPI

The OpenAPI specification is available [here](../../../services/oid4vp-task/deploy/openapi/openapi.yaml).

## Configuration

The following environment variables must be configured:

| Environment Variable                         | Description                                | Default                |
| -------------------------------------------- | ------------------------------------------ | ---------------------- |
| `GAIA_X_OID4VP_ISSUER_URL`                   | The base URL of the service                |                        |
| `GAIA_X_OID4VP_FINISH_REDIRECT_URI`          | The finish redirect URL of the service     | Base URL + `/finished` | 
| `GAIA_X_DID_USEUNIRESOLVER`                  | Whether to use universal resolver           | false                  |
| `GAIA_X_DID_RESOLVER_URL`                    | The URL of the resolver service            |                        |
| `GAIA_X_DID_REGISTRAR_URL`                   | The URL of the registrar service           |                        |
| `QUARKUS_REST_CLIENT_TRAIN_API_URL`          | The URL of the TRAIN service               |                        |
| `QUARKUS_REST_CLIENT_PROFILE_API_URL`        | The URL of the profile service             |                        | 


The following environment variables are optional:

| Environment Variable                                                | Description                                          | Default                 |
| ------------------------------------------------------------------- | ---------------------------------------------------- |------------------------ |
| `QUARKUS_LOG_LEVEL`                                                 | The log level.                                       | WARN                    |
| `QUARKUS_HTTP_HOST`                                                 | HTTP host to bind to.                                | localhost               |
| `QUARKUS_HTTP_PORT`                                                 | HTTP port to bind to.                                | 8093                    |
| `QUARKUS_CACHE_CAFFEINE__SUPPORTED_CREDENTIALS__EXPIRE_AFTER_WRITE` |  Cache for storing result for supported credentials. | 60s                     |
| `SMALLRYE_JWT_NEW_TOKEN_ISSUER`                                     | Issuer for JWT                                       | Base URL of the service |
| `MP_JWT_VERIFY_ISSUER`                                              | Verify Issuer for JWT                                | Base URL of the service |
| `GAIA_X_OID4VP_CLIENT_CLIENT_ID`                                    | The client id of the service                         | Base URL of the service |
| `SMALLRYE_JWT_NEW_TOKEN_LIFESPAN`                                   | The token lifespan                                   | 300                     |
| `GAIA_X_OID4VP_REQUEST_OBJECT_LIFETIME`                             | The request object lifetime                          |                         |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
