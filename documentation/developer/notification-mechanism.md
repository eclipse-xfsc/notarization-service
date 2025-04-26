# Notification Mechanism

<!-- TOC GitLab -->

- [Requestor Channel](#requestor-channel)
  - [onAccepted](#onaccepted)
  - [onRejected](#onrejected)
  - [onPendingDid](#onpendingdid)
  - [onExternalTask](#onexternaltask)
  - [onContactUpdate](#oncontactupdate)
  - [onDeleted](#ondeleted)
  - [onTerminated](#onterminated)
- [Operator Channel](#operator-channel)

<!-- /TOC -->

In the notarization system, the `request-processing` service uses CloudEvents over AMQP as message queue system.
Overall, two channels are used:

* requestor-request-changed
* operator-request-changed

The first channel is used to notify the requestor about changes, the second one for the operator.
A third-party component can subscribe on those channels to receive notifications on ongoing processes.

## Requestor Channel

For the requestor channel, the `session identifier` will be used as routing key.
The following messages are sent over this channel:

### onAccepted

```json
{
    "id": "<SESSIONID>",
    "msg": "REQUEST_ACCEPTED",
    "payload": null
}
```

### onRejected

```json
{
    "id": "<SESSIONID>",
    "msg": "REQUEST_REJECTED",
    "payload": null
}
```

### onPendingDid

```json
{
    "id": "<SESSIONID>",
    "msg": "REQUEST_ACCEPTED_PENDING_DID",
    "payload": null
}
```

### onExternalTask

```json
{
    "id": "<SESSIONID>",
    "msg": "EXTERNAL_TASK_STARTED",
    "payload": "<URI>"
}
```

The `onExternalTask` event is received when the requestor has to continue with an external task, like identification.
The payload would contain the URI where the actual identification is performed.

### onContactUpdate

```json
{
    "id": "<SESSIONID>",
    "msg": "CONTACT_UPDATE",
    "payload": "<CONTACT>"
}
```

In this message, the payload represents contact details of the requestor.

Notice: This event is maybe used to provide contact details to dedicated notification systems such as a microservice that validates email addresses and propagates this and other events via email.

### onDeleted

```json
{
    "id": "<SESSIONID>",
    "msg": "REQUEST_DELETED",
    "payload": null
}
```

### onTerminated

```json
{
    "id": "<SESSIONID>",
    "msg": "REQUEST_TERMINATED",
    "payload": null
}
```

## Operator Channel

For the operator channel, the `request identifier` concatenated with `.` and `profile identifier` is used.

```json
{
    "id": "<REQUEST_ID>",
    "msg": "READY_FOR_REVIEW",
    "profileId": "<PROFILE_ID>"
}
```
