@ext1
Feature: CP.NOTAR.E1.00017 Credential Issuing extension

    The existing SSI issuance service needs to be enhanced with a new protocol to issue verifiable
    credentials. If a DID supports the respective endpoint the issuance service should use the OpenID for
    Verifiable credential issuance protocol [OpenID VCI] and the component can perform a Present Proof
    request before issuing defined in OpenID for Verifiable Presentations [OpenID VP]. The credential
    issuing is an asynchronous process which checks the database for confirmed request records in the
    database. All of the found confirmed records will be picked up. Within the record MUST be a linked
    DID of the participant, which is here used to establish a connection to the Credential Manager (OCM
    and PCM) of the participant.

@happypath
Scenario: The notarization system returns a Credential Offer-URL when starting the issuance with OID4VCI
  Given a business owner has submitted a notarization request 'Req1' for issuing a credential over OID4VCI.
  And there is a notarization operator for profile 'demo-vc-issuance-01-without-tasks'
  When the notarization operator claims 'Req1'
  And the notarisation operator accepts request 'Req1'
  Then the OID4VCI Offer-URL can be fetched

@happypath
Scenario: Issuing a verifiable credential by using OpenID issuance protocol
  And a business owner has submitted a notarization request 'Req1' for issuing a credential over OID4VCI.
  And there is a notarization operator for profile 'demo-vc-issuance-01-without-tasks'
  And the notarization operator claimed and accepted the request 'Req1'
  When the business owner fetches the OID4VCI-Offer-URL
  And the business owner initiates a credential issuance with OID4VCI and a ldp_vp proof
  Then the business owner will receive a credential
  And the request has the status 'issued'

@happypath
Scenario: Issuing a SD-JWT verifiable credential by using OpenID issuance protocol
  And a business owner has submitted a notarization request 'Req1' for issuing a credential over OID4VCI for profile 'demo-vc-sd-jwt'.
  And there is a notarization operator for profile 'demo-vc-sd-jwt'
  And the notarization operator claimed and accepted the request 'Req1'
  When the business owner fetches the OID4VCI-Offer-URL
  And the business owner initiates a credential issuance with OID4VCI and a jwt proof
  Then the business owner will receive a SD-JWT credential
  And the request has the status 'issued'

@happypath
Scenario: Issuing a verifiable credential with a JsonWebSignature2020 proof by using OpenID issuance protocol
  Given I am an administrator with the username 'admin' and password 'admin'
  And there is an initialized profile 'demo-vc-jsonwebsignatures2020' with the signature type 'JsonWebSignature2020'
  And a business owner has submitted a notarization request 'Req1' for issuing a credential over OID4VCI for profile 'demo-vc-jsonwebsignatures2020'.
  And there is a notarization operator for profile 'demo-vc-jsonwebsignatures2020'
  And the notarization operator claimed and accepted the request 'Req1'
  When the business owner fetches the OID4VCI-Offer-URL
  And the business owner initiates a credential issuance with OID4VCI and a JsonWebSignature2020 ldp_vp proof
  Then the business owner will receive a credential
  And the request has the status 'issued' or 'terminated'
