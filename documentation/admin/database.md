# Database configuration

The following microservices require PostgreSQL databases:

- `ACA-Py` (one for the issuer and one for the holder)
- `compliance-task`
- `keycloak`
- `oid4vci`
- `oidc-identity-resolver`
- `profile`
- `request-processing`
- `revocation`
- `scheduler`
- `train_enrollment`

Information about the installation of PostgreSQL can be found [here](https://www.postgresql.org/docs/current/admin.html).

The connection URL and the credentials of the database user must be provided to the services via environment variables or command arguments.

It is also possible to use multiple database management systems.

## High availability, consistency and encryption

PostgreSQL features transactions with atomicity, consistency, isolation and durability (ACID) properties, ensures high availability through replications and offers encryption at several levels to protect data from disclosure.
For more details, take a look at the [documentation of PostgreSQL](https://www.postgresql.org/docs/current).

It is important to mention that identity data of the requestors are already stored encrypted.
Only authorized notarization operators are able to decrypt those data, take a look at security keys and mechanisms in the [operations guide](./operations.md) for more information.

## TLS-protected communication

For PostgreSQL, TLS can be enabled by following the required steps mentioned in the [documentation](https://www.postgresql.org/docs/current/ssl-tcp.html).
For the notarization system services that are using a database, the JDBC connection URL must be updated accordingly by enabling the SSL flag and setting some additional properties, like the SSL root certificate, see [here](https://jdbc.postgresql.org/documentation/ssl/).
