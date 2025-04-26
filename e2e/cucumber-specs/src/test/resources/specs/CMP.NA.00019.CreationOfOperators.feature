@base @notary
Feature: CMP.NA.00019 Creation of Notarization Operator Identities
  An administrator is able to create an identity for the notarization operator. This identity gets the permissions
  and rights to manage notarization requests.

@happypath
Scenario: Notarization operator can be created.
  When an administrator creates an operator with the username 'operator-username' and password 'operator-pw-123' for the profile 'demo-vc-issuance-01-without-tasks'
  Then the operator is able to login and retrieve an access token

@happypath @security
Scenario: Notarization operator is able to claim notarization requests when he is authorized.
  Given a business owner has submitted a notarization request 'RequestForClaiming' for the profile 'demo-vc-issuance-01-without-tasks' that is ready for review
  And the request 'RequestForClaiming' has the status 'readyForReview'
  And I am an operator with the username 'operator-username' and password 'operator-pw-123' for the profile 'demo-vc-issuance-01-without-tasks'
  When the notarization operator claims 'RequestForClaiming'
  Then the response has the status code 204

@happypath @security
Scenario: Notarization operator is not able to claim notarization requests of other profiles.
  Given a business owner has submitted a notarization request 'RequestForClaiming' for the profile 'demo-vc-issuance-01-without-tasks' that is ready for review
  And the request 'RequestForClaiming' has the status 'readyForReview'
  And I am an operator with the username 'operator-username' and password 'operator-pw-123' for the profile 'demo-vc-issuance-01-simple-portal'
  When the notarization operator claims 'RequestForClaiming'
  Then the response has the status code 403
