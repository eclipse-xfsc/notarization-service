@base @requestor @happypath
Feature: CMP.NA.00023 Puts the request to review
  The business owner may put the request as READY_FOR_REVIEW.

Background:
  Given I submitted a notarization request for the portal

Scenario: Portal request is on hold if the business owner is unidentified
  Given I am not identified
  When I mark my request done
  Then the response has the status code 400
  And the request has the status 'editable'

@happypath
Scenario: Request can be finalized if the business owner is identified
  And I am identified
  When I mark my request done
  Then the response has the status code 200
  Then the request has the status 'readyForReview'
