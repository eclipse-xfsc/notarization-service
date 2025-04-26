@base @notary
Feature: CMP.NA.00029 Rejection of Requests
  The notarization operator views a request with wrong data. The notarization operator is able to reject the request,
  so that the requestor can fix the wrong data.

Background:
  Given a business owner has submitted a notarization request 'RequestForClaiming' that is ready for review
  Given the request 'RequestForClaiming' has the status 'readyForReview'
  Given there is a notarization operator
  When the notarization operator claims 'RequestForClaiming'
  Then the request 'RequestForClaiming' has the status 'workInProgress'

@happypath
Scenario: Notarization operator rejects a request.
  When the notarization operator rejects the request 'RequestForClaiming'
  Then the response has the status code 204
  Then the request 'RequestForClaiming' has the status 'editable'

@happypath
Scenario: Notarization operator rejects a request and RabbitMQ receives an appropriate message about the rejection.
  When the notarization operator rejects the request 'RequestForClaiming'
  Then the response has the status code 204
  And the requestor should receive a notification about a rejected request in at least 5 seconds
