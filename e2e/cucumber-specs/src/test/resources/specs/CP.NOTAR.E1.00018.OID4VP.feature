@ext1
Feature: CP.NOTAR.E1.00018 Proof of Credentials extension

    To prove the trustworthiness of this product extension, it MUST support the OpenID for Verifiable
    Presentations and it MUST be configurable which proofs are fulfilled automatically (e.g., based on a
    configured schema). If a DID supports the respective endpoint, the SSI service should use the OpenID
    for Verifiable presentations protocol. In the case of a JSON-LD Presentation an extended validation
    MUST be supported via TRAIN [IDM.TRAIN]. After the cryptographic proofing the semantic validation
    MUST happen via TRAIN. This SHOULD validate the Trust against the Trust Framework and registries.

    Different tests required:
      -  configuration for automatic proof.
      - JSON-LD inside JWT
      - TRAIN Proof included and TL
      - etc.

@happypath
Scenario: Checking if an OID4VP task exists
  Given a profile exists called 'demo-vc-oid4vp-train'
  When I create a notarization request session for the profile 'demo-vc-oid4vp-train'
  Then there is an OID4VP task that must be fulfilled

@happypath
Scenario: Proving the trustworthiness of verifiable presentations with OID4VP
  Given a profile exists called 'demo-vc-oid4vp-train'
  And a business owner created a notarization request session for the profile 'demo-vc-oid4vp-train'
  And there is an OID4VP task that must be fulfilled
  When I follow the steps for OID4VP and create a verifiable presentation for profile 'demo-vc-oid4vp-train'
  Then the validation is successful and the OID4VP task is fulfilled
  And I am able to submit a notarization request
