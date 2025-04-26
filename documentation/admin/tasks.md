
<!-- TOC -->

- [Work](#work)
    - [Fulfilling tasks](#fulfilling-tasks)
- [Actions](#actions)
    - [Performing actions](#performing-actions)

<!-- /TOC -->

# Work

Any single notarization request may delegate work to a configured extension service, such as for the purpose of:

- identifying the requestor via OIDC
- identifying the requestor via a VP (Verifiable Presentation) via OIDVP or DIDComm
- validating the credential data against policies
- performing TRAIN enrollment after issuing a VC (Verifiable Credential)

A **task** is work performed by the requestor before completely submitting the notarization request.

A **action** is work performed by either the notary operator or an automated service either before or after the VC (Verifiable Credential) is issued.

The development or configuration of extension services is outlined in the developer documentation [here](../developer/extension-services.md).

## Fulfilling tasks

Tasks which are defined in the `taskDescription` element can be structured within the values of the fields `tasks` and `preconditionTasks` and are referenced by their name.
Both of these elements can contain a tree of tasks that has to be fulfilled before a `notarization request` can be marked as ready.
`precondition tasks` have to be fulfilled before any data can be submitted or `tasks` can be started.
In the profile shown above the trees each contain only one task:

```json
    tasks:
        {
            "taskName" : "DocumentUpload"
        }
    preconditionTasks:
        {
            "taskName" : "eID-Validation"
        }
```

A tree is fulfilled when there is one path from root to leaf containing fulfilled tasks. 
Here the `preconditionTasks` would be fulfilled when the eID-Validation is fulfilled.
The possibility to define trees allows however to create more complex scenarios:

```json
 preconditionTasks:
    {
        "oneOf": [
            { "taskName" : "eID-Validation" },
            { "taskName" : "VC-Validation" }
        ]
    }
```
This would allow the identification via one of both methods and once one of the tasks is fulfilled, the whole tree is fulfilled.
In contrast to `oneOf` it is also possible to define `allOf`, which means that all contained subtrees have to be fulfilled.
Both `oneOf` and `allOf` are lists of trees and can be nested arbitrarily.
It is also possible to define optional tasks, by using `oneOf` in combination with an empty tree node, since empty nodes are regarded as fulfilled:

```json
    tasks:
    {
        "oneOf": [
            { "taskName" : "DocumentUpload" },
            {}
        ]
    }
```

# Actions

The properties `actionDescriptions`, `preIssuanceActions` and `postIssuanceActions` contain information about the actions that are performed by the notary, or by the service on behalf of the notary.
`actionDescriptions` are used to define actions and describe them.

The development of new action types is outlined in the developer documentation [here](../developer/extension-services.md).

## Performing actions

Tasks which are defined in the `actionsDescription` element can be used within `preIssuanceActions` and `postIssuanceActions` and are referenced by their name.
Only the field `preIssuanceActions` can contain a tree of actions that has to be fulfilled before a `notarization request` can be marked as ready.
`preIssuanceActions` have to be fulfilled before any data can be submitted or `actions` can be started.
In the profile shown above the trees each contain only one task:

```json
    preIssuanceActions:
        {
            "taskName" : "DocumentUpload"
        }
    postIssuanceActions:
        [
            "train-enrollment"
        ]
```

A tree is fulfilled when there is one path from root to leaf containing fulfilled actions. 
Here the `preIssuanceActions` would be fulfilled when the eID-Validation is fulfilled.
The possibility to define trees allows however to create more complex scenarios:

```json
 preIssuanceActions:
    {
        "oneOf": [
            { "taskName" : "eID-Validation" },
            { "taskName" : "VC-Validation" }
        ]
    }
```
This would allow the identification via one of both methods and once one of the actions is fulfilled, the whole tree is fulfilled.
In contrast to `oneOf` it is also possible to define `allOf`, which means that all contained subtrees have to be fulfilled.
Both `oneOf` and `allOf` are lists of trees and can be nested arbitrarily.
It is also possible to define optional actions, by using `oneOf` in combination with an empty tree node, since empty nodes are regarded as fulfilled:

```json
    preIssuanceActions:
    {
        "oneOf": [
            { "taskName" : "DocumentUpload" },
            {}
        ]
    }
```
