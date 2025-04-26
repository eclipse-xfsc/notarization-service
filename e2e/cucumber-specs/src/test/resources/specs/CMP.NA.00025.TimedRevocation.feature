@base @requestor
Feature: CMP.NA.00025 Revocation of Notarization Requests without identification within given time
  The system must delete notarization requests with no identifcation data after a time expiration

@happypath
Scenario: There is no update for a session within given time
  Given there is a submission session with request 'Req1'
  And the maximum time period for session updates for 'Req1' is over
  When the scheduler sent the cleanup trigger
  Then the request 'Req1' has the status 'terminated'
