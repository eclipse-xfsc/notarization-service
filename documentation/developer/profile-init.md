# Profile initialization

## Initialization

Before using profiles for credential issuing they must be initialized.
That has different meaning depending on the AIP (Aries Interop Protocol) a profile is intended for.

### AIP 1.0

For AIP 1.0 profiles the initialization process includes:

1. Creation of a credential schema - the base semantic structure that describes the list of attributes which one particular Credential can contain.
2. Creation of a credential definition - the object that contains data required for credential issuance as well as credential validation.

### AIP 2.0

For AIP 2.0 profiles the initialization process includes:

1. Creation of an issuing DID which is used as the value for `issuer` field of issued credentials
2. Creation of a revocation DID which is used as an issuer of a credential status list.

## Retrieving initialization data

After a profile was initialized, its data could be retrieved by other services using the API route:

`GET http://profile/api/v1/profiles/<PROFILE_ID>/ssi-data`

The response is different depending on the AIP

### AIP 1.0

```json
{
    "did": "Th7MpTaRZVRYnPiabds81Y",
    "schemaId": "Th7MpTaRZVRYnPiabds81Y:2:demo-aip10:1.0.0",
    "credentialDefinitionId": "Th7MpTaRZVRYnPiabds81Y:3:CL:9:default"
}
```

### AIP 2.0

```json
{
    "issuingDid": "did:key:z6MkgRH1uargnMwQnrEYUfPAubFNemSri2XpMWXAhGJ5EwvW",
    "revocatingDid": "did:key:z6MkofDTVSTp4vsLY5qn8XWzNYHi1vV7TSsAb2YS5JMYPQPd"
}
```
