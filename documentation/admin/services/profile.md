# Profile Service

The `profile` service manages the configured profiles, which define the parameters and constraints of a notarization process.
For the `profile` service, the actual profiles must be mounted into the container.
Profile examples can be found in `services/profile/src/main/resources/application-dev.yaml` or in `services/profile/deploy/config/application-example-docker-compose.yaml`.

## OpenAPI

The OpenAPI specification is available [here](../../../services/profile/deploy/openapi/openapi.yaml).

## Configuration

The following properties are part of a profile description:

| Property           | Description                                                                                                                                              |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| name               | Unique name of the profile.                                                                                                                              |
| aip                | The Aries Interop Profile used (`1.0` or `2.0`). Deprecated: 'kind' is now used. '1.0' will be automatically mapped to 'AnonCred', '2.0' to 'JSON-LD'    |
| kind               | The kind of the issued credential ('AnonCred' or 'JSON-LD')                                                                                              |
| id                 | Unique identifier of the profile.                                                                                                                        |
| description        | Description of the profile.                                                                                                                              |
| notaries           | The notaries which are allowed to work on notarization requests created with this profile                                                                |
| notaryRoles        | A list of required roles which are required to manage notarization requests for a specific profile.                                                      |
| valid-for          | The validity period for credentials created with this profile                                                                                            |
| is-revocable       | The issued credential can be revoked. Is `true` by default.                                                                                              |
| task-descriptions  | Description of tasks which might have to be fulfilled before marking a request as ready                                                                  |
| tasks              | Tree of tasks which defines a set or subset of tasks in logical associations, which have to be fulfilled before marking a notarization request as ready. |
| precondition-tasks | Same as `tasks` except that the tree have to be fulfilled before submission of the request is possible.                                                  |
| template           | Template for the data which can be submitted within the notarization request.                                                                            |

These properties are described in more detail in [../profiles.md](../profiles.md).

Those profiles can be provided via `ConfigMap` and should be mounted as volume into the `Profile` pod (to `/home/jboss/config/application.yaml`).
An example ConfigMap is provided in `documentation/admin/services/profile-config-example.yaml`.

Finally, the following environment variables should be used:

| Environment Variable                                | Description                      | Default   |
| --------------------------------------------------- | -------------------------------- | --------  |
| QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT | The OTLP endpoint to connect to. | `<empty>` |

The following environment variables can be used:

| Environment Variable                                    | Description                                                       | Default   |
| ------------------------------------------------------- | ----------------------------------------------------------------- | --------- |
| QUARKUS_HTTP_ACCESS_LOG_ENABLED                         | If access logging is enabled.                                     | false     |
| QUARKUS_LOG_LEVEL                                       | The default log level.                                            | INFO      |
| QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PEM_CERTS | Comma-separated list of the trust certificate files (Pem format). | `<empty>` |
| QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE                    | The datasource pool maximum size.                                 | `<empty>` |

**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config
