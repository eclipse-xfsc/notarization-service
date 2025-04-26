<!-- TOC -->

- [Extension SSI-Issuance Services](#extension-ssi-issuance-services)
    - [Extension Service API](#extension-service-api)

<!-- /TOC -->

# Extension SSI-Issuance Services

The structuring of tasks or actions is described in the profile documentation [../admin/profiles.md](../admin/profiles.md).

## Extension SSI-Issuance API


To implement the Extension Service API, the implementing service must provide two endpoints. The fixed endpoint is used to initiate the use of the service. The second endpoint is given in the response: a service-specific callback provided to cancel the single process. 

The OpenAPI specification of the fixed endpoint is [task_service_openapi.yml](task_service_openapi.yml)

The supported interactions between the `request-processing` service and the extension service are presented below:

```mermaid
sequenceDiagram
participant ReqSub as Request Processing Service
participant IssuanceService as SSI Issuance Service

Note over ReqSub, IssuanceService: Begin new issuance process
ReqSub->>IssuanceService: POST (success callback, failure callback, content)
activate IssuanceService
IssuanceService->>IssuanceService: allocate resources, persist state
IssuanceService-->>ReqSub: redirect, cancel
deactivate IssuanceService
alt success
Note over ReqSub, IssuanceService: On completion in the Extension Service
IssuanceService->>ReqSub: POST success(success content)
ReqSub-->>ReqSub: 
else failure
Note over ReqSub, IssuanceService: On failure in the Extension Service
IssuanceService->>ReqSub: POST failure(failure content)
else cancelled
Note over ReqSub, IssuanceService: On cancellation in the Notarization-API
ReqSub->>IssuanceService: POST cancel
end
```


```mermaid
---
title: W3C Credential Creation
---
flowchart TB
    template-->applytemplate
    merge-->augmentCredential
    validFor-->augmentCredential
    issuerDid-->augmentCredential
    System-->applytemplate
    subgraph Profile
        template["Document Template"]
        validFor["Valid for"]
        issuerDid["Issuer DID"]
    end
    subgraph System

        documentHashes["Document Hashes"]
    end
    subgraph Requestor
        credentialSubjectCandidate["Credential Subject Candidate"]
        did["Holder DID"]
    end
    Requestor-->augment
    Notary-->applytemplate
    subgraph Notary
        credentialAugmentation["Credential Augmentation"]
    end
    subgraph createCredential [Create Credential]
        applytemplate["Apply Template"]
        merge["Merge subject"]
        augment["Augment subject"]
        applytemplate-->merge
        augment-->merge
    end
    subgraph Issue Credential
        augmentCredential["Prepare Credential"]

    end
```
