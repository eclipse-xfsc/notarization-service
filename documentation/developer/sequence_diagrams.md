# Processes

<!-- TOC GitLab -->

- [Session Management](#session-management)
- [Request submission content management](#request-submission-content-management)
- [Upload evidence documents](#upload-evidence-documents)

<!-- /TOC -->

## Session Management

These calls demonstrate how administrative operations are executed.

```mermaid
sequenceDiagram
    autonumber
    actor R as Requestor
    participant RP as Request-Processing
    participant MQ as RabbitMQ

    Note over R,RP: Initiate a new submission session
    R->>RP: POST /api/v1/session (profileId)
    activate RP
    RP-->>R: token, sessionId
    deactivate RP

    Note over R,RP: Fetch the status of the session
    R->>RP: GET /api/v1/session/{sessionId} (accessToken)
    activate RP
    RP-->>R: state, tasks
    deactivate RP

    Note over R,RP: Assign contact for notifications
    R->>RP: PUT /api/v1/session/{sessionId}/updateContact (accessToken, contact)
    activate RP
    RP-->>MQ: CONTACT_UPDATE
    RP-->>R: state, tasks
    deactivate RP

    Note over R,RP: Mark the request ready
    R->>RP: POST /api/v1/session/{sessionId}/ready (accessToken)
    activate RP
    RP-->>MQ: READY_FOR_REVIEW
    RP-->>R: statusCode: 200
    deactivate RP

    Note over R,RP: Remove the ready mark from the request
    R->>RP: POST /api/v1/session/{sessionId}/unready (accessToken)
    activate RP
    RP-->>R: state, tasks
    deactivate RP

    Note over R,RP: Delete the session and the notarization request
    R->>RP: DELETE /api/v1/session/{sessionId} (accessToken)
    activate RP
    RP-->>MQ: REQUEST_DELETED
    RP-->>R: state, tasks
    deactivate RP
```

## Request submission content management

These calls submit the request-specific content.

```mermaid
sequenceDiagram
    autonumber
    actor R as Requestor
    participant RP as Request-Processing

    Note over R,RP: Submit the content of the credentials to be issued

    alt Include all information
        R->>RP: POST /api/v1/session/{sessionId} (accessToken, data, invitation, holder)
        activate RP
        RP-->>R: requestId
        deactivate RP
    else Partial information
        R->>RP: POST /api/v1/session/{sessionId} (accessToken, data)
        activate RP
        RP-->>R: requestId
        deactivate RP

    end

    Note over R,RP: Update the request
    R->>RP: PUT /api/v1/session/{sessionId}/submission (accessToken, data)
    activate RP
    RP-->>R: requestId
    deactivate RP

    Note over R,RP: (Re-)assign the DID holder and invitation
    R->>RP: PUT /api/v1/session/{sessionId}/did-holder (accessToken, didHolder, invitation)
    activate RP
    Note right of R: The requestor may assign the DID holder and invitation at almost any time.
    RP-->>R: requestId
    deactivate RP
```

## Upload evidence documents

These calls demonstrate the submission of evidence documents.

```mermaid
sequenceDiagram
    autonumber
    actor R as Requestor
    participant RP as Request-Processing
    participant DSS as DSS

    Note over R,RP: Start the upload task, initiating an upload endpoint
    R->>RP: POST /api/v1/session/{sessionId}/task (taskId)
    activate RP
    RP-->>R: uploadUrl
    deactivate RP
    Note over R,RP: Upload the documents to the given upload endpoint
    loop Upload documents
        alt By link
            R->>RP: POST /api/v1/document/{sessionId}/{taskId}/uploadByLink (token, location, title, mimetype, shortDescription, longDescription)
        else By upload
            R->>RP: POST /api/v1/document/{sessionId}/{taskId}/upload (token, content, title, mimetype, shortDescription, longDescription)
        end
        activate RP

        RP-->>DSS: request verification report
        DSS-->RP: request verification report
        RP-->R: statusCode=2>
    end
    Note over R,RP: Delete an upload
    R->>RP: DELETE /api/v1/session/{sessionId}/{documentId} (taskId)
    activate RP
    RP-->>R: statusCode: 204
    deactivate RP
    Note over R,RP: Finish uploading documents
    R->>RP: POST /api/v1/session/{sessionId}/{taskId}/
    activate RP
    RP-->>R: statusCode: 204
    deactivate RP
```
