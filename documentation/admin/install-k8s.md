# Installation on Kubernetes

<!-- TOC GitLab -->

- [Installation prerequisites](#installation-prerequisites)
- [Deployment](#deployment)
- [Further information](#further-information)

<!-- /TOC -->

The installation on Kubernetes is recommended for productive environments.
Before the actual deployment can happen, some preconditions must be met.

## Installation prerequisites

- A shared PostgreSQL database.

  Please see the [database configuration section](database.md) for more information.

- A shared RabbitMQ instance.

  Please see the [message queue configuration section](message-queue.md) for more information.

- Identity provider.

  Please see the [notary authentication and authorization configuration section](auth.md) for more information.

- Aries Cloudagent.

  Please see the [acapy configuration section](acapy.md) for more information.

- Jaeger and OpenTelemetry.

  Please see the [monitoring configuration section](./monitoring.md) for more information.

- Digital Signature Service.

  Please see the [dss section](./dss.md) for more information.

## Deployment

Kubernetes Resources are provided in the directory `deploy/k8s`.
Take a look at the `README` there to understand how to deploy the notarization system.

The following services need to be configured:

- [Compliance Task](services/compliance-task.md)
- [OIDC-Identity-Resolver Service](services/oidc-identity-resolver.md)
- [Profile Service](services/profile.md)
- [Request-Processing Service](services/request-processing.md)
- [Revocation](services/revocation.md)
- [SSI-Issuance](services/ssi.md)
- [SSI-Issuance2](services/ssi2.md)
- [Scheduler](services/scheduler.md)
- [Train Enrollment](services/train-enrollment.md)
- [OID4VCI](services/oid4vci.md)
- [OID4VP-Task](services/oid4vp-task.md)

## Further information

Further information about the deployment, setting up ingress, protecting endpoints with TLS and more, can be found in the [../../deploy/k8s/README.md](../../deploy/k8s/README.md).
