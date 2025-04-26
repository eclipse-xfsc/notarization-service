@base @notary
Feature: CMP.NA.00027 Set Requests to Work in Progress
  The notarization operator picks records over the request management endpoint,
  which locks the record in the database.

@happypath
Scenario: Notarisation operator marks request as work in progress
  Given a business owner has submitted a notarization request 'RequestForClaiming' that is ready for review
  And I am an operator
  When I claim 'RequestForClaiming'
  Then the request 'RequestForClaiming' has the status 'workInProgress'

Scenario Outline: Notarisation operator can't claim requests which are not ready
  Given the request 'RequestForClaiming' with the status '<status>'
  Given I am an operator
  When I claim 'RequestForClaiming'
  Then the request 'RequestForClaiming' has the status '<status>'

  Examples:
      | status |
      | editable |
      | terminated |
