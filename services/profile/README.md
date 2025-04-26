# Profile

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## OpenAPI

The OpenAPI specification is available [here](https://gitlab.eclipse.org/api/v4/projects/4717/jobs/artifacts/main/raw/services/profile/deploy/openapi/openapi.yaml?job=compile-services).

## Prerequisites

* Java21 installed with JAVA_HOME configured appropriately
* Optionally the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) if you want to use it

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.


## Running Locally via Docker Compose

| Description | Docker Compose Run Command                                               |
|-------------|--------------------------------------------------------------------------|
| JVM Java 21 | `docker-compose -f deploy/docker-compose/java.yml up --remove-orphans` |


## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/profile-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.
