# Credential types

Supported:
- ldp_vc

Unsupported:
- jwt_vc_json
- jwt_vc_json-ld
- mso_mdoc
- vc+sd-jwt

# Revocation Status List

## W3C Model

Supported:
- StatusList2021

Unsupported:
- BitstringStatusList

# Credential Signing Algorithms

The credential signing algorithm is one of the following:
- `Ed25519Signature2018`
- `Ed25519Signature2020`
- `BbsBlsSignature2020`


# Proofs

Supported Proof Types:
- ldp_vp

Unsupported proof types
- jwt
- cwt


## Linked Data Proof Algorithms

The proof type parameter is one of the following:
- `Ed25519Signature2018`
- `Ed25519Signature2020`
- `BbsBlsSignatureProof2020`

Unsupported algorithms are:
- `RsaSignature2018`
  - `EcdsaSec   p256k1Signature2019`
- `EcdsaSecp256k1RecoverySignature2020`
- `JsonWebSignature2020`
- `GpgSignature2020`
- `JcsEd25519Signature2020`
- `DataIntegrityProof` with cryptosuite
  - `ecdsa-rdfc-2019`
  - `ecdsa-jcs-2019`
  - `ecdsa-sd-2023`
  - `eddsa-rdfc-2022`

Proofs von PCM
```asciidoc
{
  "@context":[
    "https://www.w3.org/2018/credentials/v1",
    "https://w3id.org/security/suites/jws-2020/v1","https://schema.org"
  ],
  "credentialSubject":{
    "hash":"QmbXgQJ67fawbWTHNQWjxT3KriaTopVXXc1fu9KTJkWPpS",
    "id":"uuid:2632367287r82729",
    "trustlistURI":"https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/bob.trust.train1.xfsc.dev/trust-list",
    "trustlisttype":"XML based Trust-lists"
  },
  "issuanceDate":"2024-02-24T11:40:23.746394198Z",
  "issuer":"did:jwk:eyJjcnYiOiJQLTI1NiIsImtpZCI6InRlc3QiLCJrdHkiOiJFQyIsIngiOiJJZ2xyUktTSU53eXhybzZzVDRXS3ktbW93RFcyaW8zYjNqTDlMTUw4YS1BIiwieSI6IklROGw2MS13VjBtSDRORF9PLWhFY3ItOFNZMXU4RWl2eWJMZU1IM2FfYk0ifQ",
  "proof":{
    "created":"2024-02-24T11:40:23.768488916Z",
    "jws":"eyJhbGciOiJFUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..MEUCIQDmWRONj87XFxBarpggdWpyRBYGZC_DRDCWPOaAktcDswIgceUBBMzdzKV-nGR2zAj8pjtdwQltH81F2QGeR7xZcsk",
    "proofPurpose":"assertionMethod",
    "type":"JsonWebSignature2020",
    "verificationMethod":"did:jwk:eyJjcnYiOiJQLTI1NiIsImtpZCI6InRlc3QiLCJrdHkiOiJFQyIsIngiOiJJZ2xyUktTSU53eXhybzZzVDRXS3ktbW93RFcyaW8zYjNqTDlMTUw4YS1BIiwieSI6IklROGw2MS13VjBtSDRORF9PLWhFY3ItOFNZMXU4RWl2eWJMZU1IM2FfYk0ifQ#0"
  },
  "type":"VerifiableCredential"}
```

# OID4VCI

Implemented Draft 13.

Supported grant types:
- urn:ietf:params:oauth:grant-type:pre-authorized_code
  - The `tx_code` parameter is not used

## Token Request

- Client authentication is not used

## Token Response

- c_nonce is always used
- `credential_configuration_id` is always used, so `format` is never returned

## Credential Request

- The use of `credential_identifier` is mandatory, so `format` must not be used
- credential encryption is supported, but optional
  - allowed `alg` values: `RSA1_5`, `RSA-OAEP`, `RSA-OAEP-256`, `ECDH-ES`, `ECDH-ES+A128KW`, `ECDH-ES+A192KW`, `ECDH-ES+A256KW`
  - allowed `enc` values: `A128CBC-HS256`, `A192CBC-HS384`, `A256CBC-HS512`, `A128GCM`, `A192GCM`, `A256GCM`
