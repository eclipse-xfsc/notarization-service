<!-- TOC -->

- [Profiles](#profiles)

<!-- /TOC -->

The following yaml-code shows a shortened example for a profile with which `AnonCreds` credentials can be issued:
```yml
- name: GAIA-X Employee Credential
    kind: AnonCred
    id: demo-gaia-x-employee-credential-v1
    description: This credential is used for employees in the GAIA-X demonstration.
    notaries:
     [...] (see admin documentation for details on this  part)
    is-revocable: true
    valid-for: P1Y
    task-descriptions: >
        [
            {
                "name": "eID-Validation",
                "type": "browserIdentificationTask",
                "description": "Identification via browser using eID means."
            },
            [...]
            
        ]
    tasks: >
        {
            "taskName" : "DocumentUpload"
        }
    precondition-tasks: >
        {
            "taskName" : "eID-Validation"
        }
    document-template: >
        {
            "evidenceDocument": <documents:{doc|"<doc.sha.hex>"}; separator=" ,">
        }
    template: >
        {
            "attributes": [
                "Claims",
                [...]
            ]
        }
```

The following yaml-code shows a shortened example for a profile with which `JSON-LD` credentials can be issued:
```yml
            - name: Simple Portal VC without Tasks
              kind: JSON-LD
              id: demo-vc-issuance-01-without-tasks
              description: [...]
              notaries:
                  - jwk: >
                        {[...] }
              valid-for: P100Y
              is-revocable: true
              task-descriptions: >
                []
              tasks: >
                {}
              precondition-tasks: >
                {}
              template: >
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/citizenship/v1",
                    "https://w3id.org/vc/status-list/2021/v1"
                  ],
                  "type": ["VerifiableCredential", "PermanentResidentCard"],
                  "credentialSubject": {
                    "type": "PermanentResident"
                  }
                }
```


A profile is identified by the `id` property.
This identifier must be used when a business owner is submitting a `notarization request`. 
Only notaries that have the profile ID as role are allowed to view and review `notarization requests` from this profile.

The property `notaries` contains JWKs that are used to encrypt the identity data of the business owner.
The private key is stored by the notary or the notary client.
For more information about the key handling, see [./admin/operations.md](./admin/operations.md).

With the property `is-revocable` it is indicated if the resulting credential can be revoked or not.
The property `valid-for` indicates how long the verifiable credential is valid and can be used.

The task-related fields `taskDescriptions`, `tasks`, `preconditionTasks` and the action related fields `actionDescriptions`, `preIssuanceActions`, `postIssuanceActions` are explained seperately in detail, see [./tasks.md](./tasks.md).

The `template` property is used to define the schema of the JSON data that are submitted in a `notarization request`.
In the example above, data of an employee is needed.

Note that for JSON-LD credentials the template has to contain all context URIs within the `@context` property which are needed to describe the credential which will be issued.
If the credential is revocable, the context must contain `https://w3id.org/vc/status-list/2021/v1` to describe the keys used in the `credentialStatus` property for example.

In older versions the profiles for different credential types where distinguished by the property: `ip` with values `1.0` and `2.0`, which is now deprecated.
The new property `kind` with values: `AnonCred` and `JSON-LD` is now used.
The framework can however consume profiles with `aip` or profiles with `kind` as property.
`aip: 1.0` will be mapped to `kind: AnonCred` and `aip: 2.0` to `kind: JSON-LD`.

A `notarization request` for the template above can look like the following:

```json
{
    "data": {
        "Claims" : "admin",
        "FederationName" : "Simple Federation",
        "EmpId" : "5989240124",
        "FederationId": "30129",
        "EmpEmail" : "example@email.com",
        "EmpFirstName" : "Jane",
        "EmpLastName" : "Doe"
    },
    "holder": "did:key:z6MkokM7AWa3dQvhtyLKPp2b4SsHTrAy4CWqquUA9Bmd8Khy",
    "invitation": "http://own.wallet?c_i=eyJAdHlwZSI6ICJkaWQ6c292OkJ"
}
```

The property `document-template` is optional and can be used in conjunction with the `DocumentUpload` task.
With `document-template` it is possible to make the hash of an uploaded document part of the resulting credential.
Thereby, it is possible to define how the hash can look like.
In the example above, `sha` indicates that an SHA-256 hash will be provided.
Currently, there are no other hash methods supported.
In the example above, the SHA-256 hash will be printed in the hex format.
In general, the following formats are available:

* hex
* base64
* base64URLSafe

If the business owner uploaded several documents, the hashes are separated by the separator which is provided in the `document-template` property.

The notarization system also supports chained credentials that allow data in a verifiable credential (VC) to be traced back to its origin while retaining its verifiable quality.
The [Aries RFC 0104](https://github.com/hyperledger/aries-rfcs/blob/main/concepts/0104-chained-credentials/README.md) provides some further information to chained credentials.

Further examples of profiles, including one with chained credentials, are provided [here](./services/profile-config-example.yaml)
