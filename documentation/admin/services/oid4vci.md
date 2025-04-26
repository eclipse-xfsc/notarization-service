# OID4VCI service

The `oid4vic` service implements the API for `OpenID for Verifiable Credential Issuance`
The service implements Draft 13.
An overview of supported features is given in [../oid4vci_supported_methods.md](../oid4vci_supported_methods.md).

## OpenAPI

The OpenAPI specification is available [here](../../../services/oid4vci/deploy/openapi/openapi.yaml).

## Configuration

The following environment variables must be configured:

| Environment Variable                         | Description                                | Default   |
| -------------------------------------------- | ------------------------------------------ | --------- |
| `GAIA_X_OID4VCI_ISSUER_URL`                  | The external URL of the service            |           |
| `QUARKUS_REST_CLIENT_PROFILE_API_URL`        | The bae URL of the profile service         |           |


The following environment variables are optional:

| Environment Variable                                                | Description                                          | Default                                                                            |
| ------------------------------------------------------------------- | ---------------------------------------------------- | ---------------------------------------------------------------------------------- |
| `QUARKUS_LOG_LEVEL`                                                 | The log level.                                       | WARN                                                                               |
| `QUARKUS_HTTP_HOST`                                                 | HTTP host to bind to.                                | localhost                                                                          |
| `QUARKUS_HTTP_PORT`                                                 | HTTP port to bind to.                                | 8088                                                                               |
| `QUARKUS_CACHE_CAFFEINE__SUPPORTED_CREDENTIALS__EXPIRE_AFTER_WRITE` |  Cache for storing result for supported credentials. | 60s                                                                                |
| `SMALLRYE_JWT_NEW_TOKEN_ISSUER`                                     | Issuer for JWT                                       | Base URL of the service                                                            |
| `MP_JWT_VERIFY_ISSUER`                                              | Verify Issuer for JWT                                | Base URL of the service                                                            |
| `GAIA_X_OID4VCI_CODE_LIFETIME`                                      | JWT Code lifetime                                    | PT5M                                                                               |
| `GAIA_X_OID4VCI_AT_LIFETIME`                                        | JWT Access Token lifetime                            | PT30M                                                                              |
| `GAIA_X_OID4VCI_ENCRYPT_TOKENS`                                     | Wether JWT encryption is enabled                     | true                                                                               |
| `MP_JWT_DECRYPT_KEY_ALGORITHM`                                      | JWT decryption key algorithms                        | ECDH-ES,ECDH-ES-A128KW,ECDH-ES-A192KW,ECDH-ES-A256KW,A128GCMKW,A192GCMKW,A256GCMKW |

