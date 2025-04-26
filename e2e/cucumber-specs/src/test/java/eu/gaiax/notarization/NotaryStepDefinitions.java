package eu.gaiax.notarization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.notarization.domain.AIP_1_0_Credential;
import eu.gaiax.notarization.domain.Credential;
import eu.gaiax.notarization.domain.Profile;
import eu.gaiax.notarization.environment.*;
import eu.gaiax.notarization.environment.notary.*;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;

import jakarta.inject.Inject;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.Assertions;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Neil Crossley
 */
public class NotaryStepDefinitions {

    private static final Logger LOG = LoggerFactory.getLogger(NotaryStepDefinitions.class);

    @Inject
    PersonManagement personManagement;

    @Inject
    RequestManagement requestManagement;

    @Inject
    ProfileManagement profileManagement;

    @Inject
    HttpResponseManagement httpManagement;

    @Inject
    HolderManagement holderManagement;

    @Inject
    CredentialHolder credHolder;

    @Inject
    KeycloakManagement keycloakManagement;

    @Inject
    RequestSubmissionApi requestSubmission;

    @Inject
    RequestFetchApi requestFetchApi;

    @Inject
    RequestClaimApi requestClaimApi;

    @Inject
    RequestRejectApi requestRejectApi;

    @Inject
    RequestDeleteApi requestDeleteApi;

    @Inject
    RequestAcceptApi requestAcceptApi;

    @Inject
    RequestIdentityApi requestIdentityApi;

    @Inject
    RequestAvailableDocApi requestDocApi;

    @Inject
    DocumentManagement docManagement;

    @Inject
    RabbitMQConsumer rabbitMQConsumer;

    @Inject
    RequestRevokeApi requestRevocationApi;

    @Inject
    RevocationApi revocationApi;

    @Inject
    TrainEnrollment trainEnrollment;

    @Inject
    Configuration config;

    @Inject
    ObjectMapper mapper;
    @Inject
    RequestListActionsApi requestListActionsApi;

    @Inject
    VerifiableCredentialGenerator verifiableCredentialGenerator;

    @Given("I am an operator")
    @When("there is a notarization operator")
    public void i_am_an_operator() {
        personManagement.iAmAnOperator(profileManagement.profileIdWithoutTasks().id());
    }

    @Given("I am an operator for profile {string}")
    @Given("there is a notarization operator for profile {string}")
    public void i_am_an_operator(String profileId) {
        personManagement.iAmAnOperator(profileId);
    }

    @Given("I am an administrator with the username {string} and password {string}")
    public void i_am_an_administrator(String username, String password) {
        personManagement.iAmAnAdmin(username, password);
    }

    @Given("I have a profile {string} with an Indy credential schema prepared")
    public void i_have_a_profile_with_an_indy_credential_schema_defined(String profileId) {
        profileManagement.createIndyProfile(profileId);
    }

    @Given("I have a profile {string} with a JSON-LD credential schema prepared")
    public void i_have_a_profile_with_a_json_ld_credential_schema_prepared(String profileId) {
        profileManagement.createJSONLDProfile(profileId);
    }

    @Given("I have a profile {string} with an EBSI credential schema prepared")
    public void i_have_a_profile_with_an_ebsi_credential_schema_prepared(String profileId) {
        profileManagement.createEBSIJSONLDProfile(profileId);
    }

    @Given("there is an initialized profile {string} with the signature type {string}")
    public void there_is_an_initialized_profile_with_signature_type(String profileId, String signatureType) {
        if (signatureType.equals("JsonWebSignature2020")) {
            profileManagement.createAndSubmitJsonWebSignatureProfile(profileId);
        } else {
            throw new IllegalArgumentException("Illegal signature type provided.");
        }
    }

    @When("I submit this profile to the notarization system")
    public void i_submit_this_profile_to_the_notarization_system() {
        profileManagement.submitProfile();
    }

    @Then("I am able to fetch the profile {string}")
    public void i_am_able_to_fetch_the_profile(String profileId) {
        profileManagement.canFetchProfile(profileId);
    }

    @Given("I am an operator with the username {string} and password {string} for the profile {string}")
    @When("an administrator creates an operator with the username {string} and password {string} for the profile {string}")
    public void i_am_an_operator_with_the_username_and_password_for_the_profile(String username, String password, String profile) {
        personManagement.anOperator(username, password, profile);
    }

    @Then("the operator is able to login and retrieve an access token")
    public void the_operator_is_able_to_login_and_retrieve_an_access_token() {
        httpManagement.lastResponse
                .assertThat()
                .body("access_token", Matchers.is(Matchers.notNullValue()));
    }

    @When("the administrator deletes the operator {string}")
    public void the_notarisation_operator_with_the_username_and_password_will_be_deleted(String username) {
        keycloakManagement.deleteUser(username);
    }

    @When("the administrator locks the operator {string}")
    public void the_operator_will_be_locked(String username) {
        keycloakManagement.lockUser(username);
    }

    @When("the administrator unlocks the operator {string}")
    public void the_operator_will_be_unlocked(String username) {
        keycloakManagement.unlockUser(username);
    }

    @Given("I am a locked operator with the username {string} and password {string} for the profile {string}")
    public void i_am_a_locked_operator_with_the_username_and_password_for_the_profile(String username, String password, String profile) {
        personManagement.anOperator(username, password, profile);
        keycloakManagement.lockUser(username);
    }

    @Then("the operator {string} with the password {string} for the profile {string} cannot login anymore")
    public void the_notarization_operator_cannot_login_anymore(String username, String password, String profile) {
        var user = new KeycloakManagement.User(username, password);
        var bearerToken = keycloakManagement.getBearerToken(user, Arrays.asList(profile));
        Assertions.assertNull(bearerToken);
    }

    @When("I view available requests")
    @When("the notarisation operator views all available requests")
    @When("the notarization operator views claimable requests")
    public void i_view_available_requests() {
        requestFetchApi.fetchAllAvailableRequests();
    }

    @When("the notarization operator views all own claimed requests")
    public void the_notarisation_operator_views_all_own_claimed_requests() {
        requestFetchApi.fetchOwnClaimedRequests();
    }

    @Then("I can see at least {int} requests")
    @Then("the notarization operator can see at least {int} requests")
    public void i_can_see_requests(Integer expectedAmount) {
        var amountOfRequests = requestFetchApi.amountOfRequests();
        Assertions.assertTrue(amountOfRequests >= expectedAmount);
    }

    @Then("the request {string} should be returned")
    public void the_request_should_be_returned(String requestName) {
        var submittedRequestId = requestManagement.requestId(requestName);
        Assertions.assertTrue(requestFetchApi.containsRequestWithId(submittedRequestId));
    }

    @When("I claim the request {string}")
    @Given("I claimed the request {string}")
    public void the_notarisation_operator_claims_a_named_notarization_request(String requestName) {
        requestClaimApi.claimNotarizationRequest(
                requestManagement.requestId(requestName),
                requestManagement.request(requestName).profileId().id());
    }

    @When("I claim the request")
    @When("I claim the current request")
    @When("The notarization operator claims the current request")
    @When("The notarization operator claims the request")
    public void the_notarisation_operator_claims_a_notarization_request() {
        var requestName = requestManagement.currentRequest().name();
        requestClaimApi.claimNotarizationRequest(
            requestManagement.requestId(requestName),
            requestManagement.request(requestName).profileId().id());
    }

    @Given("the notarisation operator has claimed a notarization request {string}")
    public void the_notarisation_operator_has_claimed_a_notarization_request(String requestName) {
        submitRequest(requestName);
        requestSubmission.markDone(
                personManagement.currentRequestor(),
                requestManagement.request(requestName));

        personManagement.iAmAnOperator(profileManagement.profileIdWithoutTasks().id());
        requestClaimApi.claimNotarizationRequest(
                requestManagement.requestId(requestName),
                requestManagement.request(requestName).profileId().id());
    }

    @Given("the request {string} with the status {string}")
    @Given("the request {string} with the state {string}")
    @Given("I have submitted a notarization request {string} with status {string}")
    public void the_request_with_the_status(String requestName, String state) {
        switch (state) {
            case "readyForReview" -> {
                submitRequest(requestName);
                requestSubmission.markDone(
                        personManagement.currentRequestor(),
                        requestManagement.request(requestName));
            }
            case "terminated" -> {
                submitRequest(requestName);
                requestSubmission.markDone(
                        personManagement.currentRequestor(),
                        requestManagement.request(requestName));
                personManagement.iAmAnOperator(profileManagement.profileIdWithoutTasks().id());
                requestClaimApi.claimNotarizationRequest(
                        requestManagement.requestId(requestName),
                        profileManagement.profileIdWithoutTasks().id());
                requestDeleteApi.deleteNotarizationRequest(
                        requestManagement.requestId(requestName),
                        profileManagement.profileIdWithoutTasks().id());

            }
            case "editable" -> {
                submitRequest(requestName);
            }
            case "accepted" -> {
                submitRequest(requestName);
                requestSubmission.markDone(
                        personManagement.currentRequestor(),
                        requestManagement.request(requestName));
                personManagement.iAmAnOperator(profileManagement.profileIdWithoutTasks().id());
                requestClaimApi.claimNotarizationRequest(
                        requestManagement.requestId(requestName),
                        profileManagement.profileIdWithoutTasks().id());
                requestAcceptApi.acceptNotarizationRequest(
                        requestManagement.requestId(requestName),
                        profileManagement.profileIdWithoutTasks().id());
            }
            default -> {
                String errorMsg = String.format("Submitting a request with state '%s' not implemented yet.", state);
                throw new PendingException(errorMsg);
            }
        }
    }

    @Given("a business owner has submitted a notarization request {string} that is ready for review")
    public void a_business_owner_has_submitted_a_notarization_request(String requestName) {
        submitRequest(requestName);
        requestSubmission.markDone(personManagement.currentRequestor(), requestManagement.request(requestName));
    }

    @Given("a business owner has submitted a notarization request {string} for the profile {string} that is ready for review")
    public void a_business_owner_has_submitted_a_notarization_request(String requestName, String profile) {
        submitRequest(requestName, new Profile(profile));
        requestSubmission.markDone(personManagement.currentRequestor(), requestManagement.request(requestName));
    }

    @When("the notarization operator claims {string}")
    @When("I claim {string}")
    public void the_notarisation_operator_claims(String requestName) {
        var request = requestManagement.request(requestName);
        var requestId = requestManagement.requestId(requestName);
        requestClaimApi.claimNotarizationRequest(requestId, request.profileId().id());
    }

    @Given("the notarization operator claimed and accepted the request {string}")
    public void the_notarization_operator_claimed_and_accepted_the_request(String requestName) {
        var request = requestManagement.request(requestName);
        var requestId = requestManagement.requestId(requestName);
        requestClaimApi.claimNotarizationRequest(requestId, request.profileId().id());
        requestAcceptApi.acceptNotarizationRequest(requestId, request.profileId().id());
    }

    @Given("{string} has some profile {string}")
    public void has_some_profile(String requestName, String expectedProfileId) {
        var profileId = requestManagement.request(requestName).profileId().id();
        Assertions.assertEquals(expectedProfileId, profileId);
    }

    @Given("the operator is permitted to view profile {string}")
    public void a_notarization_operator_is_permitted_to_view_profile(String expectedProfileId) {
        var profileId = personManagement.viewableProfile();
        Assertions.assertEquals(expectedProfileId, profileId);
    }

    @When("the notarization operator rejects the request {string}")
    public void the_notarization_operator_rejects_the_request(String requestName) {
        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();
        requestRejectApi.rejectNotarizationRequest(requestId, profileId);
    }

    @When("the notarization operator deletes the request {string}")
    public void the_notarization_operator_deletes_the_request(String requestName) {
        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();
        requestDeleteApi.deleteNotarizationRequest(requestId, profileId);
    }

    @When("the notarization operator fetches the identity data of request {string}")
    @When("I fetch the identity data of request {string}")
    public void the_notarization_operator_fetches_the_identity_data_of_request(String requestName) {
        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();
        requestIdentityApi.fetchAssignedIdentity(requestId, profileId);
    }

    @Then("the validation fails for request {string} and the issuer DID is not part of the resolved TRAIN result")
    public void the_validation_fails_and_the_issuer_did_is_not_part_of_the_resolved_train_result(String requestName) throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException, InvalidJwtException, JsonProcessingException {
        // We have to submit a request before a notary is able to see the TRAIN validation result
        requestSubmission.submitNotarizationRequest(requestName)
            .statusCode(201);
        this.requestSubmission.markDone(
            this.personManagement.currentRequestor(),
            requestManagement.currentRequest()
        );

        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();

        personManagement.iAmAnOperator(List.of("demo-vc-oid4vp-train-validation"));

        requestClaimApi.claimNotarizationRequest(requestId, profileId);
        requestIdentityApi.fetchAssignedIdentity(requestId, profileId);

        var encryptedValidationResult = httpManagement.lastResponse.extract().jsonPath().getString("[0].data");
        var key = createECPrivateKey(config.portalProfileDecryptionKey());

        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
            .setDecryptionKey(key)
            .setDisableRequireSignature()
            .build();

        var claims = jwtConsumer.processToClaims(encryptedValidationResult);
        var claimsRawJson = claims.getRawJson();

        var mapper = new ObjectMapper();
        var claimsJsonObj = mapper.readTree(claimsRawJson);
        var trainValidationResult = claimsJsonObj.get("validation_results").get(0).get("train_validation_result").toPrettyString();

        LOG.info(trainValidationResult);
        Assertions.assertFalse(trainValidationResult.contains(verifiableCredentialGenerator.getWebDid()));
    }

    @Then("the decrypted identity contains")
    public void the_decrypted_identity_contains(List<String> properties) throws InvalidJwtException, InvalidParameterSpecException, NoSuchAlgorithmException, InvalidKeySpecException, MalformedClaimException {
        var encryptedIdentity = httpManagement.lastResponse.extract().jsonPath().getString("[0].data");
        var key = createECPrivateKey(config.portalProfileDecryptionKey());

        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setDecryptionKey(key)
                .setDisableRequireSignature()
                .build();

        var claims = jwtConsumer.processToClaims(encryptedIdentity);

        properties.stream().forEach(expectedClaim -> {
            var isClaimAvailable =  claims.flattenClaims().containsKey("claims.claimsMap." + expectedClaim);
            if (! isClaimAvailable) {
                String errorMsg = String.format("Expected Claim is missing: %s", expectedClaim);
                Assertions.fail(errorMsg);
            }
        });
    }

    @Then("the compliance check returns a successfully signed verifiable credential for request {string}")
    public void the_compliance_check_returns_a_successfully_signed_verifiable_credential(String requestName) throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException, InvalidJwtException {
        personManagement.iAmAnOperator(config.profileIdWithComplianceCheck());

        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();

        requestClaimApi.claimNotarizationRequest(requestId, profileId);
        requestIdentityApi.fetchAssignedIdentity(requestId, profileId);

        var encryptedVC = httpManagement.lastResponse.extract().jsonPath().getString("[0].data");
        var key = createECPrivateKey(config.portalProfileDecryptionKey());

        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setDecryptionKey(key)
                .setDisableRequireSignature()
                .build();

        var claims = jwtConsumer.processToClaims(encryptedVC);
        var expectedProperties = List.of("verifiableCredential.credentialSubject", "verifiableCredential.proof.jws");
        LOG.info(claims.getRawJson());

        expectedProperties.forEach(expectedProperty -> {
            var isClaimAvailable =  claims.flattenClaims().containsKey(expectedProperty);
            if (! isClaimAvailable) {
                String errorMsg = String.format("Expected Claim is missing: %s", expectedProperty);
                Assertions.fail(errorMsg);
            }
        });
    }

    @When("the notarisation operator accepts request {string}")
    @When("I accept request {string}")
    public void the_notarisation_operator_confirms_request(String requestName) {
        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();
        requestAcceptApi.acceptNotarizationRequest(requestId, profileId);
    }

    @Given("a verifiable credential was issued for a notarization request {string}")
    public void a_verifiable_credential_was_issued(String reqName) throws InterruptedException {
        var profileId = config.profileIdTrainEnrollment();

        this.requestSubmission.createSession(reqName, new Profile(profileId));
        this.requestSubmission.submitNotarizationRequest(reqName);
        this.requestSubmission.markDone(
                this.personManagement.currentRequestor(),
                requestManagement.currentRequest()
        );

        var reqId = requestManagement.requestId(reqName);
        i_am_an_operator(config.profileIdTrainEnrollment());
        requestClaimApi.claimNotarizationRequest(reqId, profileId);
        requestAcceptApi.acceptNotarizationRequest(reqId, profileId);
        the_credential_will_be_issued_within_seconds(reqName, 20);
    }

    @When("the notarization operator submits the TRAIN enrollment data")
    public void the_notarization_operator_submits_the_train_enrollment_data() throws InterruptedException {
        the_notarization_operator_submits_the_train_enrollment_data(requestManagement.currentRequest().name());
    }

    @When("the notarization operator submits the TRAIN enrollment data for the notarization request {string}")
    public void the_notarization_operator_submits_the_train_enrollment_data(String reqName) throws InterruptedException {
        var requestId = requestManagement.requestId(reqName);
        var request = requestManagement.request(reqName);

        var actions = requestListActionsApi.listActions(requestId, request.profileId().id());

        int attempts = 0;
        do {
            Thread.sleep(200);
            actions = requestListActionsApi.listActions(requestId, request.profileId().id());
        } while(actions.isEmpty() && (++attempts) < 10);

        RequestListActionsApi.TaskInstance targetAction = null;
        if (actions.size() == 1) {
            targetAction = actions.get(0);
        } else {
            for (var item: actions) {
                var currentName = item.taskName;
                if (currentName != null && currentName.toLowerCase().contains("train")) {
                    targetAction = item;
                    break;
                }
            }
        }
        if (targetAction == null) {
            Assertions.fail("There is no action available! Thus, cannot perform the TRAIN action.");
        }

        trainEnrollment.doTrainEnrollment(targetAction);
    }

    @When("the notarization operator submits the TRAIN enrollment for {string} for a non-existant trust framework")
        public void the_notarization_operator_submits_the_train_enrollment_data_for_a_non_existant_framework(String reqName) throws InterruptedException {
            var requestId = requestManagement.requestId(reqName);
            var request = requestManagement.request(reqName);

            var actions = requestListActionsApi.listActions(requestId, request.profileId().id());

            int attempts = 0;
            do {
                Thread.sleep(200);
                actions = requestListActionsApi.listActions(requestId, request.profileId().id());
            } while(actions.isEmpty() && (++attempts) < 10);

            RequestListActionsApi.TaskInstance targetAction = null;
            if (actions.size() == 1) {
                targetAction = actions.get(0);
            } else {
                for (var item: actions) {
                    var currentName = item.taskName;
                    if (currentName != null && currentName.toLowerCase().contains("train")) {
                        targetAction = item;
                        break;
                    }
                }
            }
            if (targetAction == null) {
                Assertions.fail("There is no action available! Thus, cannot perform the TRAIN action.");
            }

            trainEnrollment.doTrainEnrollmentNonExistantFramework(targetAction);
        }

    @Given("there is a trust framework that does not exist")
    public void there_is_a_trust_framework_that_does_not_exist() {
        trainEnrollment.prepareTrustFrameworkThatDoesNotExist();
    }

    @Then("the trust framework does not exist")
    public void the_trust_framework_does_not_exist() {
        trainEnrollment.assertTrustFrameworkDoesNotExist();
    }

    @Then("the credential for {string} will be issued within {int} seconds")
    @Then("the business owner will receive a AIP 2.0 credential for {string} within {int} seconds")
    public void the_credential_will_be_issued_within_seconds(String requestName, Integer seconds) throws InterruptedException {
        var secondsToWait = seconds;
        Optional<Credential> result;
        var reqToken = requestManagement.requestToken(requestName);
        do {
            Thread.sleep(1000);
            result = holderManagement.fetchCredentialByToken(reqToken);
        } while(result.isEmpty() && secondsToWait-- > 0);

        Assertions.assertTrue(result.isPresent(),
            ()-> String.format(
                "The credential for requestName %s with token: %s was not found in returned list after %d seconds",
                requestName,
                reqToken,
                seconds
            )
        );
    }

    @Then("the aip1.0 credential for {string} will be issued within {int} seconds")
    @Then("the business owner will receive a AIP 1.0 credential for {string} within {int} seconds")
    public void the_aip10_credential_will_be_issued_within_seconds(String requestName, Integer seconds) throws InterruptedException {
        var secondsToWait = seconds;
        Optional<AIP_1_0_Credential> result;
        var reqToken = requestManagement.requestToken(requestName);
        do {
            Thread.sleep(1000);
            result = holderManagement.fetchAIP_1_0_CredentialByToken(reqToken);
        } while(result.isEmpty() && secondsToWait-- > 0);

        Assertions.assertTrue(result.isPresent(),
            ()-> String.format(
                "The credential for requestName %s with token: %s was not found in returned list after %d seconds",
                requestName,
                reqToken,
                seconds
            )
        );

        credHolder.lastAIP10Credential = result;

        // If the credential is present,we remove it immediately
        // this is necessary to not fill the holder with AIP 1.0 credentials because
        // in the credential fetch call, only 10 credentials are returned!
        result.ifPresent(c -> holderManagement.removeCredential(c.referent));
    }

    @Then("the operator is not able to perform any actions anymore")
    public void the_operator_is_not_able_to_perform_any_actions_anymore() {
        requestFetchApi.fetchAllAvailableRequests();
        httpManagement.lastResponse.statusCode(401);
    }

    @Then("the operator is able to fetch all available notarization requests")
    public void the_operator_is_able_to_fetch_all_available_notarization_requests() {
        requestFetchApi.fetchAllAvailableRequests();
        httpManagement.lastResponse.statusCode(200);
    }

    @When("I fetch the verification report for the uploaded document of the request {string}")
    public void i_fetch_the_verification_report_for_the_uploaded_document(String requestName) {
        var requestId = requestManagement.requestId(requestName);
        var profileId = requestManagement.request(requestName).profileId().id();
        var documentId = docManagement.latestUploadedDoc().id.toString();
        requestDocApi.fetchAvailableDoc(requestId, documentId, profileId);
    }

    @Then("I should retrieve the verification report")
    public void i_should_retrieve_the_verification_report() {
        var doc = requestDocApi.getLatestDoc();
        Assertions.assertNotNull(doc.verificationReport);

        var verificationReport = new String(Base64.getUrlDecoder().decode(doc.verificationReport));
        LOG.info(verificationReport);
    }

    @When("I claim and accept the request {string}")
    @When("the notarization operator claims and accepts the request {string}")
    public void i_claim_and_accept_the_request(String requestName) {
        requestClaimApi.claimNotarizationRequest(
                requestManagement.requestId(requestName),
                requestManagement.request(requestName).profileId().id());
        requestAcceptApi.acceptNotarizationRequest(
                requestManagement.requestId(requestName),
                requestManagement.request(requestName).profileId().id());
    }

    @Then("the credential for request {string} contains the document hash {string}")
    public void the_credential_contains_the_document_hash(String requestName, String docHash) {
        var reqToken = requestManagement.requestToken(requestName);
        var cred = holderManagement.fetchCredentialByToken(reqToken);

        Assertions.assertTrue(cred.isPresent());

        var evidenceDoc = cred.get().cred_value.credentialSubject.evidenceDocument;

        Assertions.assertTrue(evidenceDoc.isPresent());
        Assertions.assertEquals(docHash, evidenceDoc.get());
    }

    @Then("the aip1.0 credential for request {string} contains the document hash {string}")
    public void the_aip10_credential_contains_the_document_hash(String requestName, String docHash) {
        var cred = credHolder.lastAIP10Credential;

        Assertions.assertNotNull(cred);
        Assertions.assertTrue(cred.isPresent());

        var evidenceDoc = cred.get().attrs.evidenceDocument;

        Assertions.assertTrue(evidenceDoc.isPresent());
        Assertions.assertEquals(docHash, evidenceDoc.get());
    }

    @Then("the operator should receive a notification about a new submitted request in at least {int} seconds")
    public void the_operator_should_receive_a_notification_about_a_new_submitted_request(int timeoutSecs) {
        Assertions.assertTrue(rabbitMQConsumer.checkForOperatorMessage("READY_FOR_REVIEW", timeoutSecs));
    }

    @Then("the requestor should receive a notification about a rejected request in at least {int} seconds")
    public void the_requestor_should_receive_a_notification_about_a_rejected_request(int timeoutSecs) {
        Assertions.assertTrue(rabbitMQConsumer.checkForRequestorMessage("REQUEST_REJECTED", timeoutSecs));
    }

    private void submitRequest(String requestName) {
        var profile = profileManagement.profileIdWithoutTasks();
        submitRequest(requestName, profile);
    }

    private void submitRequest(String requestName, Profile profile) {
        requestSubmission.createSession(requestName, profile);
        requestSubmission.submitNotarizationRequest(requestName);
    }

    private ECPrivateKey createECPrivateKey(String rawBase64UrlEncodedKey) throws NoSuchAlgorithmException,
            InvalidParameterSpecException, InvalidKeySpecException {
        var decodedKey = Base64.getUrlDecoder().decode(rawBase64UrlEncodedKey);

        var kf = KeyFactory.getInstance("EC");
        var parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp384r1"));

        var ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        var privateSpec = new ECPrivateKeySpec(new BigInteger(1, decodedKey), ecParameters);
        var key = (ECPrivateKey) kf.generatePrivate(privateSpec);

        return key;
    }

    @When("the notary revokes the credential of request {string}")
    public void the_notary_revokes_credential(String requestName){
        holderManagement.fetchCredentialByToken(requestManagement.requestToken(requestName))
            .ifPresent(((cred)-> {
                var credJsonNode = mapper.valueToTree(cred);
                requestRevocationApi.revokeCredential(credJsonNode);
            }));
    }

    @Then("the revocation list contains the credential of {string} as revoked credential")
    public void the_credential_of_request_is_revoked(String requestName){
        // update list credential
        revocationApi.issueListCredential();

        holderManagement.fetchCredentialByToken(requestManagement.requestToken(requestName))
            .ifPresentOrElse((cred)-> {
                var statusListCredentialUri = cred.getStatusListCredentialUri();
                var index = cred.getStatusListIndex();

                Assertions.assertTrue(revocationApi.credentialRevoked(statusListCredentialUri, index), () -> "Credential not revoked");
            },()-> {
                throw new RuntimeException("credential not found");
            });
    }

    @Then("a new provider entry is in the trust list")
    public void a_new_provider_entry_is_in_the_trust_list() {
        trainEnrollment.checkIfProviderEntryExists();
    }

    @Given("A profile exists with the role {string}")
    public void aProfileExistsWithTheRoleRestrictedAccess(String role) {
        personManagement.iAmAnAdmin("admin", "admin");
        var profileId = UUID.randomUUID().toString();
        profileManagement.createJSONLDProfile(profileId, Set.of(role));
        profileManagement.submitProfile();
        personManagement.setProfileId(profileId);
    }

    @And("I am a notary operator with the role {string}")
    public void iAmANotaryOperatorWithTheRoleRestrictedAccess(String role) {
        personManagement.iAmAnOperator(List.of(role));
    }

    @And("I am a notary operator without the role {string}")
    public void iAmANotaryOperatorWithoutTheRoleRestrictedAccess(String role) {
        var partialRole = role.substring(0, Math.min(1, role.length() -3));
        var newRole = partialRole + "-" + UUID.randomUUID();
        personManagement.iAmAnOperator(List.of(newRole));
    }

    @When("I view the request")
    public void iViewTheRequest() {
        var currentRequest = requestManagement.currentRequest();
        var requestId = requestManagement.requestId(currentRequest.name());
        requestFetchApi.fetchRequest(requestId, personManagement.viewableProfile());
    }
}
