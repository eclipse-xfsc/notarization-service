# PostgreSQL

For general information, please see the [database configuration
section](../../../documentation/admin/database.md) and the [official website of
the chart](https://github.com/bitnami/charts/tree/main/bitnami/postgresql).

## Installation

After (optionally) customizing the `values.yaml` files, the PostgreSQL service
can be installed with the following command:

```bash
helm install postgres -f values.yaml oci://registry-1.docker.io/bitnamicharts/postgresql
```

PostgreSQL can be accessed on port 5432 from within the cluster via the
following DNS name:

`postgres-postgresql.<the-namespace>.svc.cluster.local`

The chart supports the usage of Persistent Volume Claims (PVC) to keep the data
across deployments.

## Database Management

To save the password of the database user "postgres" in an environment variable
for later usage, run:

```bash
export POSTGRES_PASSWORD=$(kubectl get secret --namespace not-api notarization-credentials -o jsonpath="{.data.postgresql-admin-password}" | base64 -d)
```

To connect to the database daemon, run the following command:

```bash
kubectl run postgres-postgresql-client --rm --tty -i --restart='Never' --namespace not-api --image docker.io/bitnami/postgresql:16.2.0-debian-12-r5 --env="PGPASSWORD=$POSTGRES_PASSWORD" --command -- psql --host postgres-postgresql -U postgres -d postgres -p 5432
```

To connect to the database daemon from outside the cluster, execute the following command:

```bash
kubectl port-forward --namespace not-api svc/postgres-postgresql 54321:5432 & psql --host 127.0.0.1 -d postgres -p 5432
```

## Upgrade

```bash
helm upgrade --install --set global.postgresql.auth.postgresPassword=$POSTGRES_PASSWORD postgres -f values.yaml oci://registry-1.docker.io/bitnamicharts/postgresql
```

## Create databases and users

Note: The databases "issuer" and "holder" for the ACA-Py instances are created
automatically. The names match the command line argument `--wallet-name`.

### Example

```sql
create user not_api with encrypted password '<mypass>';

create database not_keycloak;
grant all privileges on database not_keycloak to not_api;
# Necessary since PostgreSQL 15 (for the Flyway migrations, prevents "ERROR: permission denied for schema public"):
\c not_keycloak postgres
grant all on schema public to not_api;

create database test_compliance_task;
grant all privileges on database test_compliance_task to not_api;
\c test_compliance_task postgres
grant all on schema public to not_api;

create database test_oid4vci;
grant all privileges on database test_oid4vci to not_api;
\c test_oid4vci postgres
grant all on schema public to not_api;

create database test_oidc_identity_resolver;
grant all privileges on database test_oidc_identity_resolver to not_api;
\c test_oidc_identity_resolver postgres
grant all on schema public to not_api;

create database test_profile;
grant all privileges on database test_profile to not_api;
\c test_profile postgres
grant all on schema public to not_api;

create database test_request_processing;
grant all privileges on database test_request_processing to not_api;
\c test_request_processing postgres
grant all on schema public to not_api;

create database test_revocation;
grant all privileges on database test_revocation to not_api;
\c test_revocation postgres
grant all on schema public to not_api;

create database test_scheduler;
grant all privileges on database test_scheduler to not_api;
\c test_scheduler postgres
grant all on schema public to not_api;

create database test_ssi_issuance2;
grant all privileges on database test_ssi_issuance2 to not_api;
\c test_ssi_issuance2 postgres
grant all on schema public to not_api;

create database test_train_enrollment;
grant all privileges on database test_train_enrollment to not_api;
\c test_train_enrollment postgres
grant all on schema public to not_api;
```
