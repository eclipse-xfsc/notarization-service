@base @notary
Feature: CMP.NA.00031 Confirmation of Requests
  The notarization operator may confirm requests

Background:
  Given the notarisation operator has claimed a notarization request 'Req1'

@happypath
Scenario: Notarisation operator views all own claimed requests.
  When the notarization operator views all own claimed requests
  Then the response has the status code 200
  Then the request 'Req1' should be returned

@happypath
Scenario: Notarisation operator accepts a notarization request.
  When the notarisation operator accepts request 'Req1'
  Then the response has the status code 204
  And the request has the status 'accepted'
