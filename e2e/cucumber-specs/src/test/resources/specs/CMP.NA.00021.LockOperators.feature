@base @notary
Feature: CMP.NA.00021 Lock/Unlock of Notarization Operator Identities
  The administrator MUST be able to lock/unlock operators. A locked operator is not able to perform any actions. If
  unlocked, the identity is able to access everything as before.

@happypath @security
Scenario: The administrator locks an operator who is not able to perform any actions anymore.
  Given I am an operator with the username 'operator-username' and password 'operator-pw-123' for the profile 'demo-vc-issuance-01-without-tasks'
  When the administrator locks the operator 'operator-username'
  Then the operator is not able to perform any actions anymore

@happypath @security
Scenario: The administrator unlocks an operator who is now again able to perform actions.
  Given I am a locked operator with the username 'operator-username' and password 'operator-pw-123' for the profile 'demo-vc-issuance-01-without-tasks'
  When the administrator unlocks the operator 'operator-username'
  Then the operator is able to fetch all available notarization requests
