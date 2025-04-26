# eIDAS Bridge API

"The SSI eIDAS Bridge API service is linking the European Trust and Legal Framework, named eIDAS (electronic IDentification, Authentication and trust Services), with the Self-Sovereign Identification (SSI) global trust framework, based on Decentralized Identifiers, or DIDs."
([source](https://github.com/validatedid/ssi-eidas-bridge))

## Background

The goal of this document is to describe the creation of an eIDAS signature for a given payload. For that, a few settings have to be adjusted.
Helper scripts can be used to simplify the creation process.

## Prerequisites

For the setup, a Unix-like operating system is needed.
It was tested with Debian Bullseye. The following tools need to be available:

- Git
- Docker
- Docker-Compose
- curl
- xxd

## Configuration

The helper scripts use environment variables which are expected to be in an `.env` file in this directory.
The file `.env.example` can be used as a reference.

```sh
cp .env.example .env
```

Only a few parameters have to be adjusted.
A short description of each parameter can be found in the file.

## Create Docker image locally

At first, a Docker image of the SSI eIDAS Bridge has to be created locally.
Execute the following script to build the Docker image needed for later usage.

```sh
./build_eidas_bridge.sh
```

## Starting and stopping the eIDAS Bridge

The eIDAS Bridge requires a Redis instance as in-memory data store.
Both services can be started with the command below that uses the settings in the `docker-compose.yml`:

```sh
docker-compose up -d
```

## Usage of the eIDAS Bridge

First, a DID with an eIDAS QEC (Qualified Electronic Certificate) to eSeal W3C Verifiable Credentials has to be put into the data store.
For this, a PKCS12 file (\*.p12) containing a keypair is needed.
Beyond the technical limitations of the `ssi-eidas-bridge`, the policy of the European Trust and Legal Framework determines which keys must be used in production.
There is no limitation for testing.

```sh
./eidas-bridge-put-did.sh
```

The payload to sign has to be provided by a JSON file.
As before, an example file can be used as a reference.

```sh
cp payload.example.json payload.json
```

After necessary modifications, a signature can be created with the following script:

```sh
./eidas-bridge-create-signature.sh
```
