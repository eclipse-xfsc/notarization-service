# SSI Service

The `ssi-issuance` service is responsible for the issuance process of credentials.

This service conforms to the concept defining [SSI Issuance Services](../../developer/extension-ssi-issuance-services.md) within the Notarization API.

## Configuration

The following environment variables must be configured:

| Environment Variable     | Description                                | Default   |
| ------------------------ | ------------------------------------------ | --------- |
| `SSI_SERVICE_URL`        | URL where other services can find this one | `<empty>` |
| `ACAPY_API_URL`          | URL to ACA-Py Issuer Service.              | `<empty>` |
| `ACAPY_API_KEY`          | ACA-Py API key.                            | `<empty>` |
| `PROFILE_SERVICE_URL`    | URL to profile service.                    | `<empty>` |
| `REVOCATION_SERVICE_URL` | URL of the Revocation Service              | `<empty>` |

The following environment variables are optional:

| Environment Variable                            | Description                                                   | Default     |
|-------------------------------------------------|---------------------------------------------------------------|-------------|
| `NODE_ENV`                                      | Defines the environment for Node.js                           | development |
| `HTTP_HOST`                                     | HTTP host to bind to                                          | 0.0.0.0     |
| `HTTP_PORT`                                     | HTTP port to listen on                                        | 8180        |
| `LOG_LEVEL`                                     | Log level                                                     | error       |
| `LOG_BUFFER_INITIAL`                            | Buffer NestJS initial logs                                    | true        |
| `ACAPY_CONNECTIONS_WEBHOOKS_BUFFER`             | Number of webhook events to buffer                            | 100         |
| `ACAPY_CONNECTIONS_WEBHOOKS_TIMEWINDOW`         | The amount of time the buffered items will stay buffered (ms) | 60000       |
| `ACAPY_ISSUE_CREDENTIAL_V2_WEBHOOKS_BUFFER`     | Number of webhook events to buffer                            | 100         |
| `ACAPY_ISSUE_CREDENTIAL_V2_WEBHOOKS_TIMEWINDOW` | The amount of time the buffered items will stay buffered (ms) | 60000       |
| `ACAPY_PRESENT_PROOF_V2_WEBHOOKS_BUFFER`        | Number of webhook events to buffer                            | 100         |
| `ACAPY_PRESENT_PROOF_V2_WEBHOOKS_TIMEWINDOW`    | The amount of time the buffered items will stay buffered (ms) | 60000       |
