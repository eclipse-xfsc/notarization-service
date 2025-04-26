@base @requestor
Feature: CMP.NA.00024 Revocation of Notarization Requests
  The business owner may delete notarization requests and MUST perform an identifcation via eID
  ("This function MUST be on hold until a successful identification over the Electronic Identification Endpoint. If the identification is not successful, the creation of a request is not successful.")

@happypath
Scenario Outline: Business owner deletes his notarization request (profile without identification).
  Given I have submitted a notarization request 'RequestForDeletion' with status '<status>'
  When I delete my notarization request 'RequestForDeletion'
  Then the response has the status code 204
  And the request 'RequestForDeletion' has the state 'terminated'

  Examples:
    | status |
    | editable |
    | readyForReview |

@security
Scenario: Request is on hold if the business owner is unidentified (profile with identification).
  Given I have a submission session with an identification precondition
  And I am not identified
  When I submit the notarization request
  Then the response has the status code 400

@happypath
Scenario: Request can be submitted and deleted if the business owner is identified (profile with identification).
  Given I have a submission session with an identification precondition and request 'RequestForDeletion'
  And I am identified
  When I submit a notarization request 'RequestForDeletion'
  And I delete my notarization request 'RequestForDeletion'
  Then the response has the status code 204
  And the request 'RequestForDeletion' has the state 'terminated'
