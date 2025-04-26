# FAQ

## What is the purpose of the Notarization API?

The purpose of the Notarization API is to provide an authorization officer a software component to attest given master data and transform it to a digital verifiable credential representation.

## Is there a frontend available for the Notarization API?

No, the Notarization API is a backend System providing an API to issue verifiable credentials.
Frontends are specific to the business process and use cases and are therefore out of scope.

## What is the purpose of a profile?

A profile is used to define the parameters and constraints of a notarization process.
For a detailed look at profiles, see the [usage guide](admin/profiles.md).

## I want to use an eID-System, which does not work with OpenID Connect. Ho can I use it?

The task concept allows to extend the system with custom services.
In order to replace the OIDC based Identification, implement a task service using a different protocol.

## Which AIP versions are supported?

The AIP versions 1.0 and 2.0 are supported.
Also see the [interoperability notes](interoperability.md) for further details.

## Which programming languages were used for development?

Java, Kotlin and Node.js.
