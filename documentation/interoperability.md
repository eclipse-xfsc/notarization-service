# Note on Interoperability

The current version of the Notarization Service can be configured to support the following protocols, credential formats and proof types to enable interoperability.

## 1. Supported Protocols 

### 1.1 Issuing of Verifiable Credentials

The Notarization Service supports [OpenID for Verifiable Credential Issuance **(Draft 13)**](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html).

### 1.2 Presentation of Verifiable Presentations

The Notarization Service supports [OpenID for Verifiable Presentations **(Draft 20)**](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html).

## 2. Supported Credential Formats

The Notarization Service can be configured to support the following [Credential Formats](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A):

* ldp_vc
* vc+sd-jwt 

### 2.1 ldp_vc

The Notarization Service supports the Credential Format [ldp_vc](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.1.2) with the following key types and signature algorithms:

#### 2.1.1 ldp_vc Key Types

- RSA
- P256
- ES256K
- EdDSA

#### 2.1.2 ldp_vc Signature Algorithms

- Ed25519Signature2018
- Ed25519Signature2020
- JsonWebSignature2020

### 2.2 vc+sd-jwt

The Notarization Service supports the Credential Format [vc+sd-jwt](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.3) with the following key types and signature algorithms:

#### 2.2.1 vc+sd-jwt Key Types

- RSA
- P256
- ES256K
- EdDSA

#### 2.2.2 vc+sd-jwt Signature Algorithms

- RS256
- RS384
- RS512
- ES256
- ES384
- ES512
- ES256K
- PS256
- PS384
- PS512
- EdDSA

## 3. Supported Proof Types

The Notarization Service can be configured to support the following [Proof Types](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-proof-types):

### 3.1 jwt Proof Type

The Notarization Service can be configured to support the [jwt Proof Type](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-7.2.1.1) with the following key types and signature algorithms:

#### 3.1.1 jwt proof Key Types

- RSA
- P256
- P384
- P521
- ES256K
- EdDSA

#### 3.1.2 jwt proof Signature Algorithms

- RS256
- RS384
- RS512
- ES256
- ES384
- ES512
- ES256K
- PS256
- PS384
- PS512
- EdDSA






### 3.2 ldp_vp Proof Type

The Notarization Service can be configured to support the [ldp_vp Proof Type](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-7.2.1.2) with the following key types and signature algorithms:

#### 3.2.1 ldp_vp Key Types

- RSA
- P256
- P384
- P521
- ES256K
- EdDSA

#### 3.2.2 ldp_vp Signature Algorithms

- Ed25519Signature2018
- Ed25519Signature2020
- JsonWebSignature2020




