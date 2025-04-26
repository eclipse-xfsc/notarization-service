# Developer guide

<!-- TOC -->

- [Developer guide](#developer-guide)
    - [Introduction](#introduction)
    - [Project structure](#project-structure)
    - [Get started](#get-started)
        - [Build from source](#build-from-source)
        - [Creating service images](#creating-service-images)
        - [OpenAPI](#openapi)
    - [Task types](#task-types)
    - [Notification Mechanism](#notification-mechanism)
    - [BDD Tests](#bdd-tests)
    - [Load Tests](#load-tests)

<!-- /TOC -->(#openapi)
- [Task types](#task-types)
- [Notification Mechanism](#notification-mechanism)
- [BDD Tests](#bdd-tests)
- [Load Tests](#load-tests)

<!-- /TOC -->

## Introduction

This project consists of the following microservices:

- compliance-task
- oid4vci
- oidc-identity-resolver
- profile
- request-processing
- revocation
- scheduler
- ssi-issuance
- train-enrollment

The following third-party components are used:

- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Jaeger](https://www.jaegertracing.io/)
- [OpenTelemetry](https://opentelemetry.io/)
- [PostgreSQL](https://www.postgresql.org/)
- [RabbitMQ](https://www.rabbitmq.com/)
- [Hyperledger Aries Cloud Agent](https://github.com/hyperledger/aries-cloudagent-python)
- [Keycloak](https://www.keycloak.org/) (for Docker-Compose Deployment)

## Project structure

| Folder        | Description                                                                               |
|---------------|-------------------------------------------------------------------------------------------|
| ci            | Contains Gitlab CI specific files.                                                        |
| deploy        | Contains files for different kinds of deployments, such as docker-compose and k8s.        |
| documentation | Contains project documentation, such as admin, dev, design documentation.                 |
| e2e           | Contains BDD-Tests and Cucumber specification.                                            |
| scripts       | Contains some essential scripts to build docker and k8s resources.                        |
| services      | Contains the actual source code for the several services used in the notarization system. |

## Get started

### Build from source

The `services` directory contains all services used in the notarization system.
They are built either with Java or Node.js.
Each service contains its own README describing how the respective service can be built.

The services can be run locally via Docker-Compose, for more information take a look at the admin guide at [documentation/developer/install-docker-compose.md](../developer/install-docker-compose.md).

Or run from root:

```bash
# To compile all available java code:
./gradlew compile

# To compile a single service:
# ./gradlew :service:<directory>:compile
# such as
./gradlew :service:profile:compile
```

###  Testing

execute the following commands from the root directory:

```bash
./gradlew test

# To test a single service:
# ./gradlew :service:<directory>:test
# such as
./gradlew :service:profile:test
```

### Creating service images

The guide for creating images can be found [documentation/developer/images.md](./images.md)

### OpenAPI

The OpenAPI specification for the several services can be downloaded:

- [Profile](https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service/-/jobs/artifacts/main/download?job=openapi-profile)
- [Request-Processing](https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service/-/jobs/artifacts/main/download?job=openapi-request-processing)
- [OIDC-Identity-Resolver](https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service/-/jobs/artifacts/main/download?job=openapi-oidc-identity-resolver)
- [Revocation](https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service/-/jobs/artifacts/main/download?job=openapi-revocation)

Those OpenAPI specifications represent the current state of the services on the `main` branch.

## Task types

A single [profile](../admin/profiles.md) specifies the configuration of individual tasks to be performed by the requestor within a request submission process. The development of new task types to be integrated into and supported by the request submission process is outlined [here](extension-services.md).

## Notification Mechanism

In the notarization system, the `request-processing` service uses CloudEvents over AMQP as message queue system.
The `request-processing` service contributes messages over this message queue, for example when a request was accepted or rejected.

For more information about all the several messages and their data, see [here](notification-mechanism.md).

For more information about the server-side configuration, see [here](../admin/message-queue.md).

## BDD Tests

The guide for the execution of BDD tests can be found in [documentation/developer/bdd-tests.md](./bdd-tests.md).

## Load Tests

The guide for load tests can be found in [documentation/developer/load-tests.md](./load-tests.md).
