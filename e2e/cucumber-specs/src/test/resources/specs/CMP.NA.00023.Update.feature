@base @requestor
Feature: CMP.NA.00023 Update of Notarization Requests
  The business owner may update notarization requests and MUST perform an identifcation via eID.
  ("This function MUST be on hold until a successful identification over the Electronic Identification Endpoint. If the identification is not successful, the creation of a request is not successful.")

@happypath
Scenario Outline: Business owner updates an existing notarization request (profile without identification).
  Given I have submitted a notarization request 'Req1'
  When I upload new data for "Req1"
  Then the response has the status code 204

Scenario: Request is on hold if the business owner is unidentified (profile with identification).
  Given I have a submission session with an identification precondition
  And I am not identified
  When I submit the notarization request
  Then the response has the status code 400

@happypath
Scenario: Request can be submitted and updated if the business owner is identified (profile with identification).
  Given I have a submission session with an identification precondition and request 'Req1'
  And I am identified
  When I submit a notarization request 'Req1'
  And I upload new data for 'Req1'
  Then the response has the status code 204
  And the request has the status 'editable'
