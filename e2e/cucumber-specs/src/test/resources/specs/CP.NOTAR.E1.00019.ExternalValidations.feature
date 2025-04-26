@ext1
Feature: CP.NOTAR.E1.00019 External business validations

    The notarization administrator MUST have the possibility to add an external business validation. This
    MUST be an API GET request to an external system to fetch data. Next to that the service needs to
    validate the fetched data and include them in the issuance process. The verification process should
    include the Gaia-X compliance service to make additional checks.

    https://gitlab.com/gaia-x/lab/compliance/gx-compliance

@happypath
Scenario: The notarization system performs a compliance check of a provided verifiable presentation
  Given I created a notarization session 'Req1' for a profile with a compliance check as precondition
  When I perform the compliance check with a valid verifiable presentation
  Then I am able to submit a notarization request and mark it ready
  And the compliance check returns a successfully signed verifiable credential for request 'Req1'
