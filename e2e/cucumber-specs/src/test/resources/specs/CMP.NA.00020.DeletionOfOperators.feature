@base @notary
Feature: CMP.NA.00020 Deletion of Notarization Operator Identities
  The administrator MUST be able to delete an operator.

@happypath @security
Scenario: The administrator is able to delete an operator.
  Given I am an operator with the username 'operator-username' and password 'operator-pw-123' for the profile 'demo-vc-issuance-01-without-tasks'
  When the administrator deletes the operator 'operator-username'
  Then the operator 'operator-username' with the password 'operator-pw-123' for the profile 'demo-vc-issuance-01-without-tasks' cannot login anymore
  And the response has the status code 401
