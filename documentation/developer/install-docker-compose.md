# Installation via Docker Compose

<!-- TOC GitLab -->

- [Using ledger-management script](#using-ledger-management-script)
  - [Synopsis](#synopsis)
  - [Description](#description)
  - [Commands](#commands)
- [Starting the notarization system](#starting-the-notarization-system)
- [Building Keycloak and RabbitMQ](#building-keycloak-and-rabbitmq)
- [Third-party components used](#third-party-components-used)

<!-- /TOC -->

The installation via Docker Compose is only recommended for people developing on the Notarization system and is not supposed to be used in a productive environment.
This kind of installation can be easily used for a local setup of the Notarization system.
Make sure that a current version of Docker Compose is installed (>= 2.5.0), for more information about Docker-Compose, see [here](https://docs.docker.com/compose/install/).

## Using ledger-management script

The script to manage the ledger (Indy blockchain) is located at `scripts/ledger-management.sh`.
This script must be used to start a ledger, which is a prerequisite before starting the actual notarization system.

### Synopsis

```bash
$ scripts/ledger-management.sh <command> [<options>]
```

### Description

This shell script is used to control the Docker compose stack of the ledger.
Use it to start, stop, restart and rebuild parts of the ledger system.

### Commands

- `scripts/ledger-management.sh start-ledger`

    Start the local ledger (Indy blockchain)

- `scripts/ledger-management.sh stop-ledger`

    Stop the local ledger

- `scripts/ledger-management.sh cli`

    Starts a CLI client for the local ledger. This can only be called if the ledger was started (with `start-ledger` command).

- `scripts/ledger-management.sh logs`

    Show logs from docker containers

## Starting the notarization system

Before starting the actual notarization system, it is required to start the ledger (Indy blockchain).
The notarization system can be started by using `docker compose`.

For some cases, there is a single Docker-Compose file provided, for example, for BDD-Tests (see `deploy/local/docker-compose/bdd-tests.yml`).

You can start the notarization system from the project root directory by

```bash
$ docker compose -f deploy/local/docker-compose/bdd-tests.yml up [-d]
```

There are Docker-Compose files for the following system parts (located at `deploy/local/docker-compose/`):

| System Part          | File                 |
|----------------------|----------------------|
| ACA-Py Holder        | acapy-holder.yml     |
| ACA-Py Issuer        | acapy.yml            |
| Services             | services.yml         |
| GAIA-X Compliance    | compliance.yml       |
| TRAIN                | train.yml            |

## Building Keycloak and RabbitMQ

The custom images for the infrastructure can be built with the following scripts:

```bash
cd deploy/local/docker-compose/keycloak
./build_keycloak.sh
cd deploy/local/docker-compose/rabbitmq
./build_rabbitmq.sh
```

There are CI/CD pipeline jobs that can be triggered manually to build those images and push them to the Harbor registry located at `node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/not/`.

## Third-party components used

| Component                 | Version           |
|---------------------------|-------------------|
| PostgreSQL                | 14                |
| Keycloak                  | 23.0.7            |
| OpenTelemetry Collector   | 0.50.0            |
| JaegerTracing             | 1.34.1            |
| RabbitMQ                  | 3.9.17            |
| Aries Cloudagent          | py36-1.16-1_0.7.4 |
| Digital Signature Service | 5.10.1            |
| Indy Node                 | 1.12-4            |
