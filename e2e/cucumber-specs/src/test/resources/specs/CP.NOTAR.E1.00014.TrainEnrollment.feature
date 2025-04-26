@ext1 @requestor
Feature: CP.NOTAR.E1.00014 Enrollment of new issuers and authorities

  The enrollment process of new issuers is a process to set a DID and the respective configuration on a
  trustlist of a federation. This process can be started with the request endpoint and MUST be defined
  with one attribute. The request of the enrollment MUST follow the same flow as any other request.

  After the confirmation of the request from the notary the action / output MUST be a new entry on
  an existing trustlist. To add an entry on the trustlist the TRAIN enrollment module needs to be called.

Background:
  Given A profile with TRAIN enrolment exists called 'demo-train-enrollment'

@happypath
Scenario: A business owner can begin submitting a request for TRAIN enrollment
  When I create a notarization request session with TRAIN enrollment
  Then the request has the status 'created' or 'submittable'

@happypath
Scenario: A business owner can submit a request for TRAIN enrollment
  When I submit a notarization request session with TRAIN enrollment
  Then the request has the status 'readyForReview'

@happypath
Scenario: A issued TRAIN enrollment request leads to a new entry in the trustlist
  Given a verifiable credential was issued for a notarization request 'Req1'
  When the notarization operator submits the TRAIN enrollment data for the notarization request 'Req1'
  Then the response has the status code 204
  And the request has the status 'issued'
  And a new provider entry is in the trust list

Scenario: A issued TRAIN enrollment for a non-existant framework is not supported
  Given there is a trust framework that does not exist
  And a verifiable credential was issued for a notarization request 'Req1'
  When the notarization operator submits the TRAIN enrollment for 'Req1' for a non-existant trust framework
  Then the request has the status 'terminated'
  And the trust framework does not exist
