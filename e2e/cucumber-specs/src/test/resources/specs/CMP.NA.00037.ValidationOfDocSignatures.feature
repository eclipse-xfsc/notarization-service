@base @requestor
Feature: CMP.NA.00037 Validation of Document Signatures
  The validation of document signatures is necessary, when someone uploads a file with a digital file
  signature (e.g., QES).

@happypath
Scenario: A business owner uploads a document with a valid digital signature.
  Given I submitted a notarization request for the profile 'demo-document-upload'
  When I upload a valid signed document
  Then the response has the status code 204
  And the request has the status 'editable'
  And the database contains a verification report that the signature was valid proven

@happypath @security
Scenario: A business owner uploads a document with an invalid digital signature.
  Given I submitted a notarization request for the profile 'demo-document-upload'
  When I upload an invalid signed document
  Then the response has the status code 204
  And the request has the status 'editable'
  And the database contains a verification report that the signature was not valid with the reason 'urn:etsi:019102:subindication:HASH_FAILURE'

@happypath
Scenario: A notarization operator fetches the verfication report for an uploaded document.
  Given A notarization request 'RequestWithDocument' with an uploaded valid document for the profile 'demo-document-upload'
  And I am an operator for profile 'demo-document-upload'
  When I fetch the verification report for the uploaded document of the request 'RequestWithDocument'
  Then I should retrieve the verification report
  And the response has the status code 200
