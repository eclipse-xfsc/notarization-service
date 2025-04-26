@ext1
Feature: CP.NOTAR.E1.00016 DID method abstraction

    The SSI Issuance controller MUST be able to understand multiple DID methods and MUST also issue
    the Credential in the respective issuance protocol. The focus MUST lay on the DID methods EBSI,
    Sovrin (Indy), Web and Key. The notarization service MUST be enhanced with a connection to the
    EBSI ledger. Depending on the incoming request the SSI Issuance service MUST decide which
    credential exchange protocol [CP.NOTAR.00022] should be used. The selection of which one must be
    used depends on which service endpoint is available on the DID document of the requestor. The
    DID:Web method MUST be configurable for issuing credentials.

@happypath
Scenario: Issuing a credential by using 'key' DID.
  Given a business owner has submitted a notarization request 'Req1' with a DID 'key' as holder DID.
  And there is a notarization operator
  When the notarization operator claims 'Req1'
  And the notarisation operator accepts request 'Req1'
  And the business owner fetches the OID4VCI-Offer-URL
  And the business owner initiates a credential issuance with OID4VCI
  Then the business owner will receive a credential
  And the request has the status 'issued'

@happypath
Scenario: Issuing a credential with a 'sov' DID without providing an invitation URL.
  Given I am a requestor
  And I have a 'sov' DID
  And I submitted a ready request 'Req1' without invitation URL.
  When there is a notarization operator
  And the notarization operator claims and accepts the request 'Req1'
  Then the credential for 'Req1' will be issued within 20 seconds
  And the request has the status 'issued'

@happypath
Scenario: Issuing a credential by using 'web' DID.
  Given a business owner has submitted a notarization request 'Req1' with a DID 'web' as holder DID.
  And there is a notarization operator
  When the notarization operator claims 'Req1'
  And the notarisation operator accepts request 'Req1'
  And the business owner fetches the OID4VCI-Offer-URL
  And the business owner initiates a credential issuance with OID4VCI
  Then the business owner will receive a credential
  And the request has the status 'issued'
