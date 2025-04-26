@base @notary
Feature: CMP.NA.00034 Revoke of Credentials
  There must be an endpoint for management of revocation lists.

Background:
  Given the notarisation operator has claimed a notarization request 'Req1'

@happypath @security
Scenario: The credential will be revoked.
  Given the notarisation operator accepts request 'Req1'
  And the credential for 'Req1' will be issued within 20 seconds
  When the notary revokes the credential of request 'Req1'
  Then the revocation list contains the credential of 'Req1' as revoked credential
