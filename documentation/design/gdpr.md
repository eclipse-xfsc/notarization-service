
# GDPR Concerns

The Notarization System must take GDPR processing into consideration. This document describes how these concerns are addressed in its design.

These solutions are not sufficient on their own. For example, the administrator must configure an appropriate [log level](../admin/logging.md), and restrict access to those logs appropriately.

## GDPR Auditing

The microservice `request-processing` implements the following concept for GDPR-compliant auditability.

- All notarization request specific HTTP operations are annotated as `@Auditable` to include a `NotarizationRequestAction`.
- The following information is persisted for each HTTP operation, if available:
  - the request URI
  - the remote IP address,
  - the timestamp upon receiving the HTTP request,
  - the HTTP status code
  - the request session id, or
  - the request id,
  - the action (`NotarizationRequestAction`),
  - the request body (but not for the actions: `UPDATE_CONTACT`, `UPLOAD_DOCUMENT`, `FETCH_DOCUMENT`, `TASK_FINISH_SUCCESS`)
- For notary operations, the following is also persisted:
  - the request session id
  - the operator identity
- For requestor operations, the following is also persisted:
  - the request id
- For callbacks from external systems, the following is also persisted:
  - the task name

## Additional GDPR-relevant data

The attestation of new VC may require the person-relevant data of the requestor. They are provided by an identity provider, and need to be viewed by the notarizing agent before approving the notarization request. To ensure GDPR conformity:

- the request body of the action `TASK_FINISH_SUCCESS` is not included in the audit log.
- when received from the identity provider, the person-relevant data is only stored with the notarization request after encrypting with the public keys associated with the notarization profile.
- a notarizing agent may request the encrypted personal information of a claimed request. This request is included in the audit log.
- the notarizing agent must be provided secure access to a private key to decrypt the personal information. The management of these keys and access to them are out of scope of the notarization system.
- the encrypted person-relevant data is deleted as soon as the status of the notarization request has been finalized.
