# Keycloak

For general information, please see the [notary authentication and authorization
configuration section](../../../documentation/admin/auth.md) and the [official
website of the
chart](https://github.com/bitnami/charts/tree/master/bitnami/keycloak).

## Installation of the Helm chart

Note: database and user have to exist before the actual installation!

After (optionally) customizing the `values.yaml` files, the Jaeger service can be
installed with the following command:

`helm install keycloak -f values.yaml oci://registry-1.docker.io/bitnamicharts/keycloak`
