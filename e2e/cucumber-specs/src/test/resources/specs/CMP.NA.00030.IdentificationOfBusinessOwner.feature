@base @notary
Feature: CMP.NA.00030 Electronic Identification of Business Owner
  The notarization operator views the identity data of the business owner who is submitting a request.

Background:
  Given I am a requestor
  And I have a submission session with an identification precondition and request 'Req1'
  And I am identified
  And I submit a notarization request 'Req1'
  And the response has the status code 201
  And I mark my request done

@happypath @security
Scenario: Notarization operator fetches the encrypted identity data of the business owner for the claimed request.
  When I am an operator for profile 'demo-vc-issuance-01-identification-precondition'
  And I claimed the request 'Req1'
  When I fetch the identity data of request 'Req1'
  Then the response has the status code 200
  And the identity response has the fields
      | data       |
      | encryption |
      | algorithm  |
      | jwk        |

@happypath @security
Scenario: Notarization operator views the identity data of the business owner for the claimed request.
  When I am an operator for profile 'demo-vc-issuance-01-identification-precondition'
  And I claimed the request 'Req1'
  When I fetch the identity data of request 'Req1'
  Then the response has the status code 200
  And the decrypted identity contains
      | name       |
      | birthdate  |
