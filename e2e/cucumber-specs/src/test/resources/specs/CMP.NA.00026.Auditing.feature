@base @system
Feature: CMP.NA.00026 Auditing
  Storing/Update/Deletion of Requests	... MUST be recorded for auditing

@happypath
Scenario Outline: Business owner submits a request and the request must be recorded for auditing.
  Given I am a requestor
  And I have submitted a notarization request 'RequestSubmitted'
  When we view the auditing entries
  Then there should be a requestor audit entry for 'RequestSubmitted' with the action 'SUBMIT' after 2 seconds

@happypath
Scenario Outline: Business owner updates an existing notarization request which must be recorded for auditing.
  Given I am a requestor
  And I have submitted a notarization request 'RequestUpdated'
  And I uploaded new data for "RequestUpdated"
  When we view the auditing entries
  Then there should be a requestor audit entry for 'RequestUpdated' with the action 'UPDATE' after 2 seconds

@happypath
Scenario Outline: Business owner deletes his notarization request which must be recorded for auditing.
  Given I am a requestor
  Given I have submitted a notarization request 'RequestForDeletion' with status 'editable'
  When I delete my notarization request 'RequestForDeletion'
  And we view the auditing entries
  Then there should be a requestor audit entry for 'RequestForDeletion' with the action 'REVOKE' after 2 seconds

@happypath
Scenario Outline: Notarization operator claims a request and the claiming process must be recorded for auditing.
  Given a business owner has submitted a notarization request 'RequestForAuditing' that is ready for review
  And I am an operator
  When I claim 'RequestForAuditing'
  And we view the auditing entries
  Then there should be an operator audit entry for 'RequestForAuditing' with the action 'CLAIM' after 2 seconds
