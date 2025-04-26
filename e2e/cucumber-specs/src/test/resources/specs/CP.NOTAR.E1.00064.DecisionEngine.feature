@ext1 @notary
Feature: CP.NOTAR.E1.00064 Decision Engine Integration

    It MUST demonstrate that the notarization API can automatically make decisions for issuing based
    on OAW and TSA policy outcomes.

    Note: OAW and TSA MUST be integrated in the demonstration to show the functionality. The
    environment for these components will be provided by the principal for testing.

@happypath
Scenario: The notarization API can automatically make decisions for issuing based on TSA policy outcomes.
    Given there is a valid request 'Req1' for automatic rule-based notarization
    When the auto-notary is running and finds the request 'Req1'
    Then the request 'Req1' is automatically evaluated based on the TSA policy 'policies/example/PrincipalCredentialRequest/1.0'
    And the request 'Req1' is automatically accepted
    And the credential for 'Req1' will be issued within 20 seconds
    And the request has the status 'accepted' or 'issued'
