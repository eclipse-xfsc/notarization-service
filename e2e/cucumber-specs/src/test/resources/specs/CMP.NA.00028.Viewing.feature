@base @notary
Feature: CMP.NA.00028 Viewing of Requests
  The notarization operator may view all claimable requests, whereas there may be some filtering.

Background:
  Given the request 'RequestForClaiming' with the status 'readyForReview'
  And there is a notarization operator
  And 'RequestForClaiming' has some profile 'demo-vc-issuance-01-without-tasks'
  And the operator is permitted to view profile 'demo-vc-issuance-01-without-tasks'

@happypath
Scenario: Notarization operator views claimable requests and all open requests are returned.
  When the notarization operator views claimable requests
  Then the request 'RequestForClaiming' should be returned

@happypath
Scenario: Notarization operator views all own claimed requests.
  When the notarization operator claims 'RequestForClaiming'
  And the notarization operator views all own claimed requests
  Then the notarization operator can see at least 1 requests
