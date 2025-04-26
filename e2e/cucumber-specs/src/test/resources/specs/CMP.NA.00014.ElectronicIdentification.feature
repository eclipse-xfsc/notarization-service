@base @requestor @notary
Feature: CMP.NA.00014 Electronic Identification Endpoint
  The result of the identification must be legally secure.

@happypath
Scenario: The requestor is able to submit a notarization request after the notarization system was able to verify the credential of the requestor.
  Given I created a notarization session with an identification precondition
  And I created a credential for profile 'demo-vc-issuance-01-identification-precondition'
  When the credential is issued
  And I identify myself with the verifiable credential
  Then I am able to submit a notarization request
  And the response has the status code 201

@happypath @security
Scenario: The endpoint for viewing available notarization requests is protected and cannot be used by unauthorized entities.
  Given the request 'RequestForClaiming' with the status 'readyForReview'
  When I view available requests
  Then the response has the status code 401

@happypath @security
Scenario: An enabled notarization operator is able to view available notarization requests.
  Given the request 'RequestForClaiming' with the status 'readyForReview'
  And I am an operator
  When I view available requests
  Then the response has the status code 200
  And the request 'RequestForClaiming' should be returned
