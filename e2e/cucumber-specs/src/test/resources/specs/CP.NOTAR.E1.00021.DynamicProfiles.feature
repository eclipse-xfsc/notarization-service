@ext1
Feature: CP.NOTAR.E1.00021 Dynamic schema (profile) configuration

    The software extension MUST include dynamic schema configuration. For now, the credential
    schemas (profiles) can only be configured before starting the service. The software extension MUST
    include a dynamic schema configuration at run time. That means an endpoint needs to be created
    which can only be used by the admin of the software or the enabled notaries. For the generation of
    the profile the component IDM.SR should be used. Based on the different Trust anchor and credential
    format of the profile the schema regeneration should follow a different flow. The following schema
    generation flows MUST be supported:

    - EBSI schema generation with the Trusted Schemas Registry from EBSI 8
    - IDUnion (Indy) credential schema creation and credential definition
    - JSON-LD schema context generation and storage on IPFS

@happypath
Scenario: The notarization operator is able to include an EBSI profile at runtime.
  Given I am an administrator with the username 'admin' and password 'admin'
  And I have a profile 'test-ebsi' with an EBSI credential schema prepared
  When I submit this profile to the notarization system
  Then the response has the status code 204
  And I am able to fetch the profile 'test-ebsi'

@happypath
Scenario: The notarization operator is able to include an Indy profile at runtime.
  Given I am an administrator with the username 'admin' and password 'admin'
  And I have a profile 'test-indy' with an Indy credential schema prepared
  When I submit this profile to the notarization system
  Then the response has the status code 204
  And I am able to fetch the profile 'test-indy'

@happypath
Scenario: The notarization operator is able to include a JSON-LD profile at runtime.
  Given I am an administrator with the username 'admin' and password 'admin'
  And I have a profile 'test-jsonld' with a JSON-LD credential schema prepared
  When I submit this profile to the notarization system
  Then the response has the status code 204
  And I am able to fetch the profile 'test-jsonld'
