@ext1
Feature: CP.NOTAR.E1.00022 Credential Issuance

    If a DID Document provides multiple Communication protocols like OIDC4VC, Indy AIP 1.0 and
    AIP 2.0, the notarization API MUST issue a credential to all of the protocols, to have a consistent set
    of credentials in all wallets of the user.

@happypath
Scenario: Issuing multiple credentials over OIDC4VC and Indy AIP 2.0.
  Given a business owner has submitted a notarization request 'Req1' for issuing a credential over OID4VCI.
  And there is a notarization operator for profile 'demo-vc-issuance-01-without-tasks'
  And the notarization operator claimed and accepted the request 'Req1'
  When the business owner fetches the OID4VCI-Offer-URL
  And the business owner initiates a credential issuance with OID4VCI
  Then the business owner will receive a credential over OID4VCI
  And the business owner will receive a AIP 2.0 credential for 'Req1' within 20 seconds
  And the request has the status 'issued'
