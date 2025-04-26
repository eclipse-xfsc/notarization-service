<!-- TOC -->

- [Domain Driven Design](#domain-driven-design)
    - [Notarization Request Processing Domain](#notarization-request-processing-domain)
        - [Submission](#submission)
        - [Management](#management)
        - [Audit](#audit)
        - [Task Processing](#task-processing)
    - [SSI Issuance Domain](#ssi-issuance-domain)
    - [Profile Domain](#profile-domain)
    - [OIDC Identity Resolver Domain](#oidc-identity-resolver-domain)
    - [OpenID Verifiable Credential Domain](#openid-verifiable-credential-domain)
    - [TRAIN Identity Resolver](#train-identity-resolver)
    - [Notifications](#notifications)
- [External](#external)
    - [NotarizationOperatorFrontEnd](#notarizationoperatorfrontend)

<!-- /TOC -->
**Note**: the diagrams are modelled using [Context Mapper](https://contextmapper.org/) in [model.cml](model.cml). To generate the documentation from file, install [jbang](https://www.jbang.dev/) and execute [./generate_diagrams](./generate_diagrams).

# Domain Driven Design

The GAIA-X Notarization system consists of the following bounded contexts, realized as individual micro-services:

- RequestProcessingContext
- SSIIssuanceContext
- IdentificationContext
- SignatureServiceContext
- ProfileContext
- RevocationContext

The following external bounded contexts exist:

- BusinessOwnerFrontEnd
- NotarizationOperatorFrontEnd
- IntegrationTRAINBoundedContext

![Context Map of the GAIA-X Notarization Domain](./images/model_ContextMap.svg "Context Map of the GAIA-X Notarization Domain")

## Notarization Request Processing Domain

The Notarization Request Processing builds on the following core model:

![Core model of the Notarization Requests](./images/model_BC_RequestProcessingContext_NotarizationRequestCore.svg "Core model of the Notarization Requests")

A Notarization Request follows the given state diagram:

![State diagram of the notarization requests](./images/model_BC_RequestProcessingContext_NotarizationRequestSubmissionFlow_StateDiagram.svg "State diagram of the notarization requests")

### Submission

The submission of Notarization Requests requires a specialized data model for use by the BusinessOwnerFrontEnd. 

![Model for submision of Nozarization Requests](./images/model_BC_RequestProcessingContext_NotarizationRequestSubmissionAggregate.svg "Model for submision of Nozarization Requests")

### Management

The management of Notarization Requests requires a specialized data model for use by the NotarizationOperatorFrontEnd.

![Model for management of Nozarization Requests](./images/model_BC_RequestProcessingContext_NotarizationRequestManagementAggregate.svg "Model for management of Nozarization Requests")

### Audit

Audit Logging is modelled as:

![Model of audit logs](./images/model_BC_AuditLogContext_AuditLogAggregate.svg "Model of audit logs")

### Task Processing

Task processing is modelled as:

![Model of audit logs](./images/model_BC_RequestProcessingContext_NotarizationRequestTaskProcessing.svg "Model of task processing")


## SSI Issuance Domain

The SSI Issuance realizes the following aggregate for the issuance of credentials or enrolment of provider entry in trust lists:
  
![Model for requesting the issuance of credentials](./images/model_BC_SSIIssuanceContext_IssueCredentialAggregate.svg "Model for requesting the issuance of credentials")


![Model for requesting enrolment](./images/model_BC_SSIIssuanceContext_IssueEntrolmentAggregate.svg "Model for requesting the enrolment in a trust list of a provider entry")

## Profile Domain

The profile service manages the configured profiles, which define the parameters of a notarization process.

![Model of the profiles, that each configure a notarization process](./images/model_BC_ProfileContext_ProfileAggregate.svg "Model of the profiles, that each configure a notarization process")

## OIDC Identity Resolver Domain

This micro-service implements a facade to a third-party identity provider with the purpose of identifying a requestor.

![Model for requesting an identity](./images/model_BC_OidcIdentityResolverContext_IdentificationAggregate.svg "Model for requesting an identity")

## OpenID Verifiable Credential Domain

This micro-service implements a facade to a service that implements the protocols OIDC4VCI and OIDC4VP. Internally, it implements the 

![Model for requesting an identity](./images/model_BC_OidcIdentityResolverContext_IdentificationAggregate.svg "Model for requesting an identity")


## TRAIN Identity Resolver

This micro-service implements a facade to a TRAIN instance with the purpose of identifying a requestor.

## Notifications

Notifications are sent out to notarization operators to inform them about new notarization requests and to requestors about any updates to their requests.
For this purpose, the message queue system RabbitMQ is used. 

# External

## NotarizationOperatorFrontEnd

There are currently no supported solutions that fulfil the frontend requirement.

This could be fulfilled by:

- a web UI operated by a human notary
- a software agent that 
