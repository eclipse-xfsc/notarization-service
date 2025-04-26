@base @requestor
Feature: CMP.NA.00022 Technical Creation of Notarization Requests
  The business owner creates notarization requests, may upload files (see CMP.NA.00037) and MUST perform an identification via eID
  ("This function MUST be on hold until a successful identification over the Electronic Identification Endpoint. If the identification is not successful, the creation of a request is not successful.")

Scenario: Business owner creates his submission session
  Given I create a notarization request session
  Then the response has the status code 201
  And the request has the status 'submittable'

@happypath
Scenario: Business owner creates his session for the portal
  Given I create a notarization request session for the portal
  Then the response has the status code 201
  And the request has the status 'submittable'

@happypath
Scenario: Business owner submits his notarisation request for the portal
  Given I have a submission session for the portal
  When I submit a notarization request
  Then the response has the status code 201
  And the request has the status 'editable'

Scenario: Request is on hold if the business owner is unidentified
  Given I have a submission session with an identification precondition
  And I am not identified
  When I submit the notarization request
  Then the response has the status code 400
  And the request has the status 'created'

@happypath
Scenario: Request can be submitted if the business owner is identified
  Given I have a submission session with an identification precondition
  And I am identified
  When I submit a notarization request
  Then the response has the status code 201
  Then the request has the status 'editable'
