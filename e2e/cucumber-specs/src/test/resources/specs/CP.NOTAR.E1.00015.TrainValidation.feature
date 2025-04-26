@ext1
Feature: CP.NOTAR.E1.00015 TRAIN validation & verification

  The TRAIN validation service needs to be included in the process of verifying a verifiable presentation.
  If a requestor is showing a verifiable presentation during a task which needs to be fulfilled, the
  notarization service needs to validate the terms Of Use by calling the TRAIN validation module
  [IDM.TRAIN.00017].

  This will verify if the shown verifiable presentation and the respective owner of
  it is on a trust list.

@happypath
Scenario: A business owner is identifiable via TRAIN VC
  Given a profile exists called 'demo-vc-oid4vp-train-validation'
  And a business owner created a notarization request session for the profile 'demo-vc-oid4vp-train-validation'
  And there is an OID4VP-TRAIN task that must be fulfilled
  And there is a trust list with the frameworkName 'alice.trust.train1.xfsc.dev'
  When I follow the steps for OID4VP and my wallet contains a TRAIN VC for profile 'demo-vc-oid4vp-train-validation'
  Then the validation is successful and the 'OID4VP-TRAIN' task is fulfilled
  And I am able to submit a notarization request

Scenario: A business owner with non-TRAIN VC is not identifiable via TRAIN
  Given a profile exists called 'demo-vc-oid4vp-train-validation'
  And a business owner created a notarization request session 'Req1' for the profile 'demo-vc-oid4vp-train-validation'
  And there is an OID4VP-TRAIN task that must be fulfilled
  And there is a trust list with the frameworkName 'alice.trust.train1.xfsc.dev'
  When I follow the steps for OID4VP and my wallet contains a non-TRAIN VC for profile 'demo-vc-oid4vp-train-validation'
  Then the validation fails for request 'Req1' and the issuer DID is not part of the resolved TRAIN result
