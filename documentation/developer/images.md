# Container images

<!-- TOC GitLab -->

- [Java services](#java-services)
- [Node.js services](#nodejs-services)

<!-- /TOC -->

For each service a `README` is provided where it is stated how to build container images with docker.

Further details and alternatives for image building and pushing are supported by the Quarkus framework (such as Podman) are outlined: https://quarkus.io/guides/container-image

## Java services

The following services are implemented with Java:

- compliance-task
- oidc-identity-resolver
- profile
- request-processing
- revocation
- scheduler
- train-enrollment

For those services some properties are set:

```properties
quarkus.container-image.registry=registry.gitlab.com
quarkus.container-image.group=gaia-x/data-infrastructure-federation-services/not/notarization-service
quarkus.container-image.name=${quarkus.application.name}
```

In the CI-pipeline those properties are overwritten to push Docker images to the configured `Harbor` registry.
The images are currently distributed within the registry `node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/not/`.

The images can be built by using the `imageBuild` Gradle task.
This can look like the following:

```bash
$ ./gradlew :services:oidc-identity-resolver:imageBuild
```

It is recommended to pass at least the following arguments:

```bash
-Dquarkus.container-image.tag=$TAG
```

For further arguments, look at the CI/CD pipeline or at the [Quarkus documentation](https://quarkus.io/guides/container-image#quarkus-container-image_configuration).

## Node.js services

The following services are implemented with Node.js:

- ssi-issuance

This image can be built by using Gradle too:

```bash
$ TAG=latest-dev ./gradlew :services:ssi-issuance:imageBuild
```

But this requires to have access to a Docker daemon and is therefore not suitable for CI/CD pipelines (requires Docker-In-Docker runner).
For CI/CD purposes, [kaniko](https://github.com/GoogleContainerTools/kaniko) is used to build the `ssi-issuance` service.
