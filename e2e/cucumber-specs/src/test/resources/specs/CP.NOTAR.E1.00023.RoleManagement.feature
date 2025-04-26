@ext1
Feature: CP.NOTAR.E1.00023 Role Management

    The profiles MUST be decoupled from roles to allow and flexible management.

    @happypath
    Scenario: The profile role is required to view a request
        Given A profile exists with the role 'restricted-access'
        And a business owner has submitted a notarization request 'restricted-req' that is ready for review
        And I am a notary operator with the role 'restricted-access'
        When I view the request
        Then the response has the status code 200

    @happypath
    Scenario: A notary without a role required by the profile cannot view requests
        Given A profile exists with the role 'restricted-access'
        And a business owner has submitted a notarization request 'restricted-req' that is ready for review
        And I am a notary operator without the role 'restricted-access'
        When I view the request
        Then the response has the status code 403

    @happypath
    Scenario: The profile role is required to claim a request
        Given A profile exists with the role 'restricted-access'
        And a business owner has submitted a notarization request 'restricted-req' that is ready for review
        And I am a notary operator with the role 'restricted-access'
        When I claim the request
        Then the response has the status code 204

    @happypath
    Scenario: A notary without a role required by the profile cannot claim requests
        Given A profile exists with the role 'restricted-access'
        And a business owner has submitted a notarization request 'restricted-req' that is ready for review
        And I am a notary operator without the role 'restricted-access'
        When I claim the request
        Then the response has the status code 403
