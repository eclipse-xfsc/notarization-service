@base @notary
Feature: CMP.NA.00029 Deletion of Requests
  The notarization operator views a request with wrong data. The notarization operator is able to delete the request.

Background:
  Given a business owner has submitted a notarization request 'RequestForClaiming' that is ready for review
  Given the request 'RequestForClaiming' has the status 'readyForReview'
  Given there is a notarization operator
  When the notarization operator claims 'RequestForClaiming'
  Then the request 'RequestForClaiming' has the status 'workInProgress'

@happypath
Scenario: Notarization operator deletes a request.
  When the notarization operator deletes the request 'RequestForClaiming'
  Then the response has the status code 204
  Then the request 'RequestForClaiming' has the status 'terminated'
