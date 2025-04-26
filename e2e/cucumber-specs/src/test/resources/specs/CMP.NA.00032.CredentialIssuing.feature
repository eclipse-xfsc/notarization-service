@base @notary
Feature: CMP.NA.00032 Credential Issuing
  The notarization request record must be linked to the participants DID. After the operator accepts the requests,
  the credential will be issued for the given DID.

@happypath
Scenario: The credential will be issued.
  Given the notarisation operator has claimed a notarization request 'Req1'
  When the notarisation operator accepts request 'Req1'
  Then the credential for 'Req1' will be issued within 20 seconds
  And the request has the status 'issued'

@happypath
Scenario: Issuing a credential with AIP 1.0.
  Given a business owner has submitted a notarization request 'Req1' for AIP1.0
  And there is a notarization operator for profile 'demo-aip10'
  When the notarization operator claims 'Req1'
  And the notarisation operator accepts request 'Req1'
  Then the aip1.0 credential for 'Req1' will be issued within 20 seconds
  And the request has the status 'issued'

@happypath
Scenario: Issuing a credential with a private DID without providing an invitation URL.
  Given I am a requestor
  And I have a private DID
  And I submitted a ready request 'Req1' without invitation URL.
  When there is a notarization operator
  And the notarization operator claims and accepts the request 'Req1'
  Then the invitation URL is available in at least 20 seconds

@happypath
Scenario: Issuing a credential with a public DID without providing an invitation URL.
  Given I am a requestor
  And I have a public DID
  And I submitted a ready request 'Req1' without invitation URL.
  When there is a notarization operator
  And the notarization operator claims and accepts the request 'Req1'
  Then the credential for 'Req1' will be issued within 20 seconds
  And the request has the status 'issued'
