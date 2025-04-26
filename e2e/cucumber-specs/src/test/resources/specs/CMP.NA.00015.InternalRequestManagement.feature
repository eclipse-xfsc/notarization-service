@base @notary
Feature: CMP.NA.00015 Internal Request Management Endpoint
  The notarization system must inform the operator immediately for new requests. The notarization requests can be
  retrieved by GET actions. A notarization operator can confirm (see CMP.NA.00031) and reject (see CMP.NA.00024)
  requests. All request endpoints are protected by internal security protection mechanisms (see CMP.NA.00014) and
  MUST be documented by audit entries (see CMP.NA.00026).

@happypath
@focus
Scenario: When a business owner submits a notarization request for review, the operators are informed.
  Given I submitted a notarization request for the profile 'demo-vc-issuance-01-without-tasks'
  When I mark my request done
  Then the operator should receive a notification about a new submitted request in at least 5 seconds
