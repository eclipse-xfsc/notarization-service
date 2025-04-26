@base @requestor
Feature: CMP.NA.00038 Credential Schema/Evidence Link
  Issued credentials must contain the hash values of uploaded documents and may contain HTTP links to the given evidence documents.

@happypath @security
Scenario: An issued credential contains the hash value of an uploaded document (AIP 2.0).
  Given A notarization request 'RequestWithDocument' with an uploaded valid document for the profile 'demo-document-upload'
  And I am an operator for profile 'demo-document-upload'
  When I claim and accept the request 'RequestWithDocument'
  Then the credential for 'RequestWithDocument' will be issued within 20 seconds
  And the credential for request 'RequestWithDocument' contains the document hash '3a0a16dab8f903366eabcf5fc5b5e22d0aa61b50b3ac10c0a58b331402caa0e3'

@happypath @security
Scenario: An issued credential contains the hash value of an uploaded document (AIP 1.0).
  Given A notarization request 'RequestWithDocument' for AIP 1.0 with an uploaded valid document for the profile 'demo-document-upload-aip-10'
  And I am an operator for profile 'demo-document-upload-aip-10'
  When I claim and accept the request 'RequestWithDocument'
  Then the aip1.0 credential for 'RequestWithDocument' will be issued within 20 seconds
  And the aip1.0 credential for request 'RequestWithDocument' contains the document hash '3a0a16dab8f903366eabcf5fc5b5e22d0aa61b50b3ac10c0a58b331402caa0e3'
