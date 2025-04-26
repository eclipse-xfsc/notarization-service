# SSI2 Service

The `ssi-issuance2` service provides an internal api for the issuance of credentials and allows the usage of different software components capable of issueing credentials like Acapy.

This service conforms to the concept defining [SSI Issuance Services](../../developer/extension-ssi-issuance-services.md) within the Notarization API.

## OpenAPI

The OpenAPI specification is available [here](../../../services/ssi-issuance2/deploy/openapi/openapi.yaml).

## Configuration

The following environment variables must be configured:

| Environment Variable                         | Description                                | Default   |
| -------------------------------------------- | ------------------------------------------ | --------- |
| `ISSUANCE2_SERVICE_URL`                      | URL where other services can find this one | `<empty>` |
| `QUARKUS_REST_CLIENT_REVOCATION_SERVICE_URL` | URL of the Revocation Service              | `<empty>` |
| `QUARKUS_REST_CLIENT_OFFER_API_URL`          | URL of the OID4VP Service                  | `<empty>` |
| `QUARKUS_REST_CLIENT_PROFILE_API_URL`        | URL of the Profile Service                 | `<empty>` |
| `QUARKUS_REST_CLIENT_ACAPY_JSON_URL`         | URL of the ACA-Py Issuer Service           | `<empty>` |


The following environment variables are optional:

| Environment Variable                                         | Description                                                                                     | Default       |
| ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------- |-------------- |
|  `ISSUANCE2_SESSION_RETENTION_PERIOD`                        | The time period until a issuance session is cancelled automatically                             | P1D           |
|  `ISSUANCE2_SESSION_CALLFAILONTIMEOUT`                       | Wether or not automatically cancelled sessions are notified to the Request Processing Service.  | false         |
|  `QUARKUS_CACHE_CAFFEINE__PROFILE_CACHE__EXPIRE_AFTER_WRITE` | The cache duration for profiles requested from the Profile Service.                             | 10m           |
|  `QUARKUS_LOG_LEVEL`                                         | The log level.                                                                                  | WARN          |
|  `QUARKUS_HTTP_HOST`                                         | HTTP host to bind to.                                                                           | 8089          |
|  `QUARKUS_HTTP_PORT`                                         | HTTP port to bind to.                                                                           | localhost     |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
