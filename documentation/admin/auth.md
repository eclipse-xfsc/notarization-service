# Authentication and authorization configuration

In general, it must be distinguished between the identification of business owners and authorization of notarization operators.

The identification of business owners is done by using the `oidc-identity-resolver` service.
This service connects to other IdPs via OIDC, for example an eID system.
The `oidc-identity-resolver` service uses the `quarkus-oidc` library and must be configured by providing a `clientID`, `clientSecret` and an `authServerUrl`.
See [services/oidc-identity-resolver.md](./services/oidc-identity-resolver.md) for more information.

The request management endpoints in the `request-processing` service must be protected from unauthorized access.
For this purpose, an OAuth2 based IAM is required which is used by the `request-processing` service to check if a caller is authorized to access a certain management endpoint.
The `request-processing` service uses the `quarkus-elytron-security-oauth2` library and must be configured by providing a `clientID`, `clientSecret` and an `introspectionUrl`.
See [services/request-processing.md](./services/request-processing.md) for more information.

In general, there are two types of access tokens supported, opaque tokens and JWTs.

For opaque tokens, make sure that the token introspection response contains the profiles of a notary in the scope value.
Profile names may not contain space characters.

A possible identity provider can be `Keycloak` which is used in the Docker-Compose installation.
More information can be found [here](https://www.keycloak.org/guides).
