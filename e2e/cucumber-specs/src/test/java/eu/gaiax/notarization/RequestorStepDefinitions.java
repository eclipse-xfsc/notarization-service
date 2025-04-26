package eu.gaiax.notarization;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.gaiax.notarization.domain.*;
import id.walt.sdjwt.SDJwt;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import eu.gaiax.notarization.domain.RequestSession.RequestValues;
import eu.gaiax.notarization.domain.RequestSession.SessionTaskSummary;
import eu.gaiax.notarization.domain.RequestSession.SessionTaskTree;
import eu.gaiax.notarization.environment.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.*;

import static org.hamcrest.Matchers.*;

import org.jboss.logging.Logger;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Assertions;

public class RequestorStepDefinitions {

    private static Logger logger = Logger.getLogger(RequestorStepDefinitions.class);

    private static final String BROWSER_IDENTIFICATION_TASK = "eID-Validation";
    private static final String VC_IDENTIFICATION_TASK = "VC-Validation";
    private static final String COMPLIANCE_CHECK_TASK = "Compliance-Check";
    private static final String OID4VP_TASK = "OID4VP";
    private static final String OID4VP_TRAIN_TASK = "OID4VP-TRAIN";

    @Inject
    PersonManagement personManagement;

    @Inject
    RequestManagement requestManagement;

    @Inject
    RequestSubmissionApi requestSubmission;

    @Inject
    ProfileManagement profileManagement;

    @Inject
    DocumentManagement docManagement;

    @Inject
    IssuerManagement credentialManagement;

    @Inject
    HolderManagement holderManagement;

    @Inject
    TrainEnrollment trainEnrollment;

    @Inject
    OID4VCIManagement oid4vciManagement;

    @Inject
    ProofGenerator proofGenerator;

    @Inject
    EntityManager entityManager;

    @Inject
    HttpResponseManagement httpResponseManagement;

    @Inject
    Configuration config;

    @Given("I am a requestor")
    public void i_am_a_requestor() {
        personManagement.iAmARequestor();
    }

    @Given("I create a notarization request session")
    public void i_create_a_new_session_() {
        requestSubmission.createSession();
    }

    @Given("I have a submission session")
    public void i_have_a_submission_session() {
        requestSubmission.createSession()
                .statusCode(201);
    }

    @Given("there is a submission session with request {string}")
    public void i_have_a_submission_session(String name) {
        requestSubmission.createSession(name)
                .statusCode(201);
    }

    @Given("I have a submission session with an identification precondition")
    public void i_have_a_submission_session_with_identification_precondition() {
        requestSubmission.createSession(profileManagement.profileIdWithIdentificationPrecondition())
                .statusCode(201);
    }

    @Given("There is a submission session with an identification precondition and request {string}")
    @Given("I have a submission session with an identification precondition and request {string}")
    public void i_have_a_submission_session_with_identification_precondition(String requestName) {
        requestSubmission.createSession(requestName, profileManagement.profileIdWithIdentificationPrecondition())
                .statusCode(201);
    }

    @Given("I have a submission session for the portal")
    @Given("I create a notarization request session for the portal")
    @Given("I create(d)? a notarization request session for the portal")
    public void i_have_a_submission_session_for_the_portal() {
        requestSubmission.createSession(profileManagement.somePortalProfileId())
                .statusCode(201);
    }

    @When("I create a notarization request session for the profile {string}")
    @Given("a business owner created a notarization request session for the profile {string}")
    public void i_create_a_notarization_request_session_for_the_profile_demo_vc_oid4vp_train(String profileId) {
        requestSubmission.createSession(new Profile(profileId))
                .statusCode(201);
    }

    @Given("a business owner created a notarization request session {string} for the profile {string}")
    public void i_create_a_notarization_request_session_for_the_profile_demo_vc_oid4vp_train(String requestName, String profileId) {
        requestSubmission.createSession(requestName, new Profile(profileId))
            .statusCode(201);
    }

    @Then("the request has the (status|state) {status}")
    public void the_request_has_the_status_status(RequestStatus status) {
        RequestSession currentRequest = requestManagement.currentRequest();
        currentRequest.person().given().get(currentRequest.location()).then().body("state", equalTo(status.toString()));
    }

    @Then("the request has the status {string} or {string}")
    public void the_request_has_the_status_or(String status1, String status2) {
        RequestSession currentRequest = requestManagement.currentRequest();
        var state = currentRequest.person()
                .given()
                .get(currentRequest.location())
                .then()
                .extract().jsonPath()
                .getString("state");
        Assertions.assertTrue(List.of(status1, status2).contains(state));
    }

    @When("I submit a notarization request")
    @When("I submit the notarization request")
    public void i_submit_a_notarization_request() {
        requestSubmission.submitNotarizationRequest();
    }

    @Given("I submit a notarization request {string}")
    public void i_submitted_a_notarization_request(String requestName) {
        requestSubmission.submitNotarizationRequest(requestName)
                .statusCode(201);
    }

    @Given("a business owner has submitted a notarization request {string} with a DID {string} as holder DID.")
    public void a_business_owner_has_submitted_a_notarization_request_with_a_did_as_holder_did(String reqName, String didMethod) throws ExecutionException, InterruptedException {
        this.holderManagement.prepareDidForProof(didMethod);
        this.requestSubmission.createSession(reqName, new Profile(config.profileIdWithoutTasks()));
        this.requestSubmission.submitNotarizationRequest(reqName);
        this.requestSubmission.markDone(
                this.personManagement.currentRequestor(),
                requestManagement.currentRequest()
        );
    }

    @Given("a business owner has submitted a notarization request {string} for issuing a credential over OID4VCI.")
    public void a_business_owner_has_submitted_a_notarization_request_for_issuing_a_credential_over_oid4vci(String reqName) throws ExecutionException, InterruptedException, TimeoutException {
        this.holderManagement.prepareDidForProof("key");
        this.requestSubmission.createSession(reqName, new Profile(config.profileIdOid4vci()));
        requestSubmission.submitNotarizationRequest()
                .statusCode(201);
        this.requestSubmission.markDone(
                this.personManagement.currentRequestor(),
                requestManagement.currentRequest()
        );
    }

    @Given("a business owner has submitted a notarization request {string} for issuing a credential over OID4VCI for profile {string}.")
    public void a_business_owner_has_submitted_a_notarization_request_for_issuing_a_credential_over_oid4vci_for_profile(String reqName, String profileId) throws ExecutionException, InterruptedException, TimeoutException {
        this.holderManagement.prepareDidForProof("key");
        this.requestSubmission.createSession(reqName, new Profile(profileId));
        requestSubmission.submitNotarizationRequest()
                .statusCode(201);
        this.requestSubmission.markDone(
                this.personManagement.currentRequestor(),
                requestManagement.currentRequest()
        );
    }

    @When("I submit a notarization request session with TRAIN enrollment")
    public void i_submit_a_notarization_request_session_with_train_enrollment() {
        this.requestSubmission.createSession(new Profile(config.profileIdTrainEnrollment()));
        this.requestSubmission.submitNotarizationRequest();
        this.requestSubmission.markDone(
                this.personManagement.currentRequestor(),
                requestManagement.currentRequest()
        );
    }

    @Given("I created a notarization session with an identification precondition")
    @Given("I created a notarization session with any identification precondition")
    public void i_created_a_notarization_session_with_any_identification_precondition() {
        this.requestSubmission.createSession(this.profileManagement.profileIdWithIdentificationPrecondition());
    }

    @Given("I created a notarization session {string} for a profile with a compliance check as precondition")
    public void i_created_a_notarization_session_for_a_profile_with_a_compliance_check_as_precondition(String requestName) {
        this.requestSubmission.createSession(requestName, this.profileManagement.profileIdWithComplianceCheck());
    }

    @Given("I submitted a notarization request for the portal")
    public void i_submitted_a_notarization_request_requiring_identification() {
        this.requestSubmission.createSession(this.profileManagement.somePortalProfileId());
        requestSubmission.submitNotarizationRequest()
                .statusCode(201);
    }

    @Given("I submitted a notarization request for the profile {string}")
    public void i_submitted_a_notarization_request_for_the_profile(String profile) {
        this.requestSubmission.createSession(new Profile(profile));
        requestSubmission.submitNotarizationRequest()
                .statusCode(201);
    }

    @Given("I have a submission session for the profile {string}")
    public void i_have_a_submission_session_for_the_profile(String profile) {
        this.requestSubmission.createSession(new Profile(profile));
    }

    @Given("I have a submission session {string} for the profile {string}")
    public void i_have_a_submission_session_for_the_profile(String requestName, String profile) {
        this.requestSubmission.createSession(requestName, new Profile(profile));
    }

    @Given("a business owner has submitted a notarization request {string} for profile {string}.")
    public void a_business_owner_has_submitted_a_notarization_request_for_profile(String reqName, String profileId) throws ExecutionException, InterruptedException, TimeoutException {
        this.requestSubmission.createSession(reqName, new Profile(profileId));
        requestSubmission.submitNotarizationRequest()
                .statusCode(201);
        this.requestSubmission.markDone(
            this.personManagement.currentRequestor(),
            requestManagement.currentRequest()
        );
    }

    @Then("I am able to submit a notarization request")
    public void i_am_able_to_submit_a_notarization_request() {
        requestSubmission.submitNotarizationRequest(
                this.requestManagement.currentRequest())
                .statusCode(201);
    }

    @Then("I am able to submit a notarization request and mark it ready")
    public void i_am_able_to_submit_a_notarization_request_and_mark_it_ready() {
        requestSubmission.submitNotarizationRequest(
                        this.requestManagement.currentRequest())
                .statusCode(201);
        requestSubmission.markDone(
                this.personManagement.currentRequestor(),
                requestManagement.currentRequest()
        );
    }

    @When("I perform the compliance check with a valid verifiable presentation")
    public void i_perform_the_compliance_check_with_a_valid_verifiable_presentation() throws InterruptedException {
        final String taskName = COMPLIANCE_CHECK_TASK;
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

        SessionTaskSummary complianceCheckTask = findTaskByName(state, taskName);

        if (complianceCheckTask != null) {
            this.requestSubmission.executeComplianceCheckTask(
                    this.personManagement.currentRequestor(),
                    this.requestManagement.currentRequest(),
                    complianceCheckTask
            );
        }

        int attempts = 0;
        do {
            Thread.sleep(1000);
            state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

            complianceCheckTask = findTaskByName(state, taskName);
        } while(complianceCheckTask != null && (++attempts) < 10);

        assertThat("Compliance check failed.", complianceCheckTask, nullValue());
    }

    @Given("I am identified")
    public void i_am_identified() throws InterruptedException {
        final String taskName = BROWSER_IDENTIFICATION_TASK;
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

        SessionTaskSummary identificationTask = findTaskByName(state, taskName);

        if (identificationTask != null) {
            this.requestSubmission.performBrowserIdentificationTask(
                    this.personManagement.currentRequestor(),
                    this.requestManagement.currentRequest(),
                    identificationTask);
        }

        int attempts = 0;
        do {
            Thread.sleep(200);
            state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

            identificationTask = findTaskByName(state, taskName);
        } while(identificationTask != null && (++attempts) < 3);
        assertThat("I could not be identified", identificationTask, nullValue());
    }

    private SessionTaskSummary findTaskByName(RequestValues state, final String taskName) {
        RequestSession.SessionTaskSummary task = null;
        if (!state.preconditionTasksFulfilled) {
            task = findTaskByName(state.preconditionTaskTree, taskName);
        }
        if (task == null && !state.tasksFulfilled) {
            task = findTaskByName(state.taskTree, taskName);
        }
        return task;
    }

    @When("I identify myself with the verifiable credential")
    public void i_identify_myself_with_verifiable_credentials() throws InterruptedException {
        final String taskName = VC_IDENTIFICATION_TASK;
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

        var did = credentialManagement.latestCredentialDid();
        var invitationUrl = holderManagement.createInvitation();

        VcTaskStart vcTaskStart = new VcTaskStart();
        vcTaskStart.holderDID = did;
        vcTaskStart.invitationURL = invitationUrl;

        SessionTaskSummary identificationTask = findTaskByName(state, taskName);

        if (identificationTask != null) {
            this.requestSubmission.performVcIdentificationTask(
                    this.personManagement.currentRequestor(),
                    this.requestManagement.currentRequest(),
                    identificationTask,
                    vcTaskStart);
        }

        int attempts = 0;
        do {
            Thread.sleep(1000);
            state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

            identificationTask = findTaskByName(state, taskName);
        } while(identificationTask != null && (++attempts) < 10);
        assertThat("I could not be identified", identificationTask, nullValue());
    }

    @Given("I created a credential for profile {string}")
    public void i_created_a_credential(String profile) {
        credentialManagement.createCredential(profile);
    }

    @Given("I have a private DID")
    public void i_have_a_private_did() {
        holderManagement.usePrivateDid();
    }

    @Given("I have a public DID")
    @Given("I have a 'sov' DID")
    public void i_have_a_public_did() {
        holderManagement.usePublicDid();
    }

    @When("the credential is issued")
    public void the_credential_is_issued() {
        var did = credentialManagement.latestCredentialDid();
        credentialManagement.waitTilTheCredentialIsIssued(did);
    }

    @When("the maximum time period for session updates for {string} is over")
    @Transactional
    public void setModificationTimeOverdue(String name) {
        var sessId = requestManagement.request(name).sessionId();
        entityManager.createNativeQuery("update requestsession set lastModified = '1970-01-01' where id = '" + sessId + "'").executeUpdate();
    }

    @When("the scheduler sent the cleanup trigger")
    public void waitForSchedulerTrigger() throws InterruptedException {
        //We just wait
        Thread.sleep(2000);
    }

    @When("I mark my request done")
    public void i_mark_my_request_done() {
        this.requestSubmission.markDone(this.personManagement.currentRequestor(), requestManagement.currentRequest());
    }

    @Given("I have submitted a notarization request {string}")
    public void i_have_submitted_a_notarization_request(String name) {
        requestSubmission.createSession(name, profileManagement.somePortalProfileId());
        requestSubmission.submitNotarizationRequest(name);

    }

    @Given("I submitted a notarization request")
    public void i_submitted_a_notarization_request() {
        requestSubmission.createSession(profileManagement.somePortalProfileId());
        requestSubmission.submitNotarizationRequest();
    }

    @Given("there is a valid request {string} for automatic rule-based notarization")
    public void there_is_a_valid_request_for_automatic_rule_based_notarization(String reqName) {
        requestSubmission.createSession(reqName, profileManagement.profileIdWithAutoNotarization());
        requestSubmission.submitNotarizationRequest(reqName);
        requestSubmission.markDone(this.personManagement.currentRequestor(), requestManagement.currentRequest());
    }

    @Given("I am not identified")
    public void i_am_not_identified() {
        logger.warn("No identification is the default state.");
    }

    @Then("the request has the status {string}")
    public void the_request_has_the_state(String state) {
        RequestValues summary = requestSubmission.fetchSummary(requestManagement.currentRequest());
        assertThat(summary.state, is(state));
    }

    @Then("the request {string} has the status {string}")
    @Then("the request {string} has the state {string}")
    public void the_request_has_the_state(String requestName, String state) {
        RequestValues summary = requestSubmission.fetchSummary(requestManagement.request(requestName));
        assertThat(summary.state, is(state));
    }

    @When("I upload a valid signed document")
    public void i_upload_a_valid_signed_document() {
        var session = requestManagement.currentRequest();
        docManagement.uploadValidPDFDocument(session);
    }

    @When("I upload an invalid signed document")
    public void i_upload_an_invalid_signed_document() {
        var session = requestManagement.currentRequest();
        docManagement.uploadInvalidXMLDocument(session);
    }

    @Given("A notarization request {string} with an uploaded valid document for the profile {string}")
    public void a_notarization_request_with_an_uploaded_valid_document(String requestName, String profile) {
        this.requestSubmission.createSession(requestName, new Profile(profile));
        requestSubmission.submitNotarizationRequest()
                .statusCode(201);
        docManagement.uploadValidPDFDocument(requestManagement.currentRequest());
        requestSubmission.markDone(this.personManagement.currentRequestor(), requestManagement.currentRequest());
    }

    @Given("A notarization request {string} for AIP 1.0 with an uploaded valid document for the profile {string}")
    public void a_notarization_request_for_aip_with_an_uploaded_valid_document_for_the_profile(String requestName, String profile) {
        this.requestSubmission.createSession(requestName, new Profile(profile));
        var request = requestManagement.currentRequest();
        requestSubmission.submitNotarizationRequestAIP10(request.person(), request)
                .statusCode(201);
        docManagement.uploadValidPDFDocument(requestManagement.currentRequest());
        requestSubmission.markDone(this.personManagement.currentRequestor(), requestManagement.currentRequest());
    }

    @When("I create a notarization request session with TRAIN enrollment")
    public void i_create_a_notarization_request_session_with_train_enrollment() {
        this.requestSubmission.createSession(new Profile(config.profileIdTrainEnrollment()));
    }

    @Given("I submitted a ready request {string} without invitation URL.")
    public void a_business_owner_submitted_a_request_without_invitation_url(String requestName) {
        requestSubmission.createSession(requestName, profileManagement.profileIdWithoutTasks());
        requestSubmission.submitNotarizationRequestWithoutInvitation(requestManagement.request(requestName));
        requestSubmission.markDone(personManagement.currentRequestor(), requestManagement.request(requestName));
    }

    @Then("the invitation URL is available in at least {int} seconds")
    public void the_invitation_url_can_be_fetched_within_seconds(Integer timeout) {
        var invitationUrls = requestSubmission.fetchInvitationUrl(
                personManagement.currentRequestor(),
                requestManagement.currentRequest(),
                timeout);

        Assertions.assertNotNull(invitationUrls);
        Assertions.assertFalse(invitationUrls.isEmpty());
        var invitationUrl = invitationUrls.get(0).inviteUrl.toString();

        logger.infov("Invitation URL: {0}", invitationUrl);

        Pattern pattern = Pattern.compile("http://acapy:8030.*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(invitationUrl);
        Assertions.assertTrue(matcher.find());
    }

    @When("I upload new data for {string}")
    @Given("I uploaded new data for {string}")
    public void i_upload_new_data_for(String requestName) {
        requestSubmission.updateNotarizationRequest(requestManagement.request(requestName));
    }

    @When("I delete my notarization request {string}")
    public void i_delete_my_notarization_request(String requestName) {
        requestSubmission.deleteNotarizationRequest(requestManagement.request(requestName));
    }

    @Given("a business owner has submitted a notarization request {string} for AIP1.0")
    public void a_business_owner_has_submitted_a_notarization_request_for_aip(String requestName) throws InterruptedException {
        var profile = profileManagement.profileIdAIP10();

        this.requestSubmission.createSession(requestName, profile);
        this.i_am_identified();

        this.requestSubmission.submitNotarizationRequestAIP10(
                personManagement.currentRequestor(),
                requestManagement.request(requestName));
        this.requestSubmission.markDone(
                personManagement.currentRequestor(),
                requestManagement.request(requestName));
    }

    @Given("A profile with TRAIN enrolment exists called {string}")
    public void a_profile_with_train_enrolment_exists_called(String profileId) {
        this.profileManagement.canFetchProfile(profileId);
    }

    @Given("a profile exists called {string}")
    public void a_profile_exists_called(String profileId) {
        this.profileManagement.canFetchProfile(profileId);
    }

    @Then("the OID4VCI Offer-URL can be fetched")
    @When("the business owner fetches the OID4VCI-Offer-URL")
    public void the_oid4vci_offer_url_can_be_fetched() {
        oid4vciManagement.fetchOfferUrl();
    }

    @When("the business owner initiates a credential issuance with OID4VCI")
    @When("the business owner initiates a credential issuance with OID4VCI and a ldp_vp proof")
    public void the_business_owner_initiates_a_credential_issuance_with_oid4vci() throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        var credOffer = oid4vciManagement.extractCredentialOffer();

        var tokenEndpoint = oid4vciManagement.getTokenEndpoint(credOffer);
        var tokenResponse = oid4vciManagement.callTokenEndpoint(tokenEndpoint, credOffer);

        var accessToken = tokenResponse
                .extract()
                .jsonPath()
                .getString("access_token");
        var nonce = tokenResponse
                .extract()
                .jsonPath()
                .getString("c_nonce");

        var proof = proofGenerator.getProof(nonce, credOffer.credential_issuer);
        var credentialEndpoint = oid4vciManagement.getCredentialEndpoint(credOffer);
        oid4vciManagement.callCredentialEndpointWithLdpVpProof(credentialEndpoint, accessToken, proof, credOffer);
    }

    @When("the business owner initiates a credential issuance with OID4VCI and a jwt proof")
    public void the_business_owner_initiates_a_credential_issuance_with_oid4vci_with_jwt() throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        var credOffer = oid4vciManagement.extractCredentialOffer();

        var tokenEndpoint = oid4vciManagement.getTokenEndpoint(credOffer);
        var tokenResponse = oid4vciManagement.callTokenEndpoint(tokenEndpoint, credOffer);

        var accessToken = tokenResponse
                .extract()
                .jsonPath()
                .getString("access_token");
        var nonce = tokenResponse
                .extract()
                .jsonPath()
                .getString("c_nonce");

        var proof = proofGenerator.getJWTProof(nonce, credOffer.credential_issuer, credOffer.credential_issuer);
        var credentialEndpoint = oid4vciManagement.getCredentialEndpoint(credOffer);
        oid4vciManagement.callCredentialEndpointWithJWTProof(credentialEndpoint, accessToken, proof, credOffer);
    }

    @When("the business owner initiates a credential issuance with OID4VCI and a JsonWebSignature2020 ldp_vp proof")
    public void the_business_owner_initiates_a_credential_issuance_with_oid4vci_and_a_json_web_signature2020_ldp_vp_proof() throws JsonProcessingException {
        var credOffer = oid4vciManagement.extractCredentialOffer();

        var tokenEndpoint = oid4vciManagement.getTokenEndpoint(credOffer);
        var tokenResponse = oid4vciManagement.callTokenEndpoint(tokenEndpoint, credOffer);

        var accessToken = tokenResponse
            .extract()
            .jsonPath()
            .getString("access_token");
        var nonce = tokenResponse
            .extract()
            .jsonPath()
            .getString("c_nonce");

        var proof = proofGenerator.getJsonWebSignature2020Proof(nonce, credOffer.credential_issuer, credOffer.credential_issuer);
        var credentialEndpoint = oid4vciManagement.getCredentialEndpoint(credOffer);
        oid4vciManagement.callCredentialEndpointWithLdpVpProof(credentialEndpoint, accessToken, proof, credOffer);
    }

    @Then("the business owner will receive a credential")
    @Then("the business owner will receive a credential over OID4VCI")
    public void the_business_owner_will_receive_a_credential() {
        var credentialResp = httpResponseManagement.lastResponse;
        var holderDid = credentialResp.extract().jsonPath().getString("credential.credentialSubject.id");
        Assertions.assertEquals(holderDid, holderManagement.did());
    }

    @Then("the business owner will receive a SD-JWT credential")
    public void the_business_owner_will_receive_a_sd_jwt_credential() throws ParseException {
        var credentialResp = httpResponseManagement.lastResponse;
        var sdJwtRaw = credentialResp.extract().jsonPath().getString("credential");
        var sdJwt = SDJwt.Companion.parse(sdJwtRaw);
        Assertions.assertTrue(sdJwt.getFullPayload().containsKey("iss"));
        Assertions.assertTrue(sdJwt.getFullPayload().containsKey("vct"));
    }

    @Then("there is an OID4VP task that must be fulfilled")
    public void there_is_an_oid4vp_task_that_must_be_fulfilled() {
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

        SessionTaskSummary oid4vpTask = findTaskByName(state, OID4VP_TASK);

        Assertions.assertNotNull(oid4vpTask);
        Assertions.assertFalse(oid4vpTask.fulfilled);
    }

    @Then("there is an OID4VP-TRAIN task that must be fulfilled")
    public void there_is_an_oid4vp_train_task_that_must_be_fulfilled() {
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

        SessionTaskSummary oid4vpTask = findTaskByName(state, OID4VP_TRAIN_TASK);

        Assertions.assertNotNull(oid4vpTask);
        Assertions.assertFalse(oid4vpTask.fulfilled);
    }

    @Given("there is a trust list with the frameworkName {string}")
    public void there_is_a_trust_list_for(String frameworkName) {
        trainEnrollment.createTrustList(frameworkName);
    }

    @When("I follow the steps for OID4VP and create a verifiable presentation for profile {string}")
    public void i_follow_the_steps_for_oid4vp(String profile) throws InterruptedException, InvalidJwtException, MalformedClaimException {
        doOid4VP(profile, OID4VP_TASK, false);
    }

    @When("I follow the steps for OID4VP and my wallet contains a non-TRAIN VC for profile {string}")
    public void i_follow_the_steps_for_oid4vp_train(String profile) throws InterruptedException, InvalidJwtException, MalformedClaimException {
        doOid4VP(profile, OID4VP_TRAIN_TASK, false);
    }

    @When("I follow the steps for OID4VP and my wallet contains a TRAIN VC for profile {string}")
    public void my_wallet_contains_a_train_vc_for_profile(String profile) throws InvalidJwtException, MalformedClaimException, InterruptedException {
        doOid4VP(profile, OID4VP_TRAIN_TASK, true);
    }

    @Then("the validation is successful and the OID4VP task is fulfilled")
    public void the_validation_is_successful_and_the_task_is_fulfilled() {
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());
        var oid4vpTask = findTaskByName(state.preconditionTaskTree, OID4VP_TASK);

        Assertions.assertNotNull(oid4vpTask);
        Assertions.assertTrue(oid4vpTask.fulfilled);
    }

    @Then("the validation is successful and the {string} task is fulfilled")
    public void the_validation_is_successful_and_the_placeholder_task_is_fulfilled(String taskName) {
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());
        var task = findTaskByName(state.preconditionTaskTree, taskName);

        Assertions.assertNotNull(task);
        Assertions.assertTrue(task.fulfilled);
    }

    @Then("the validation fails and the OID4VP task is not fulfilled")
    public void the_validation_fails_and_the_oid4vp_task_is_not_fulfilled() {
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());
        var oid4vpTask = findTaskByName(state.preconditionTaskTree, OID4VP_TASK);

        Assertions.assertNotNull(oid4vpTask);
        Log.error("Currently the task is fulfilled because TRAIN validation is disabled in oid4vp.");
        Assertions.assertFalse(oid4vpTask.fulfilled);
    }

    private SessionTaskSummary findTaskByType(SessionTaskTree preconditionTaskTree, String targetType) {

        if (preconditionTaskTree == null) {
            return null;
        }
        if (preconditionTaskTree.task != null) {
            return Objects.equals(targetType, preconditionTaskTree.task.type)
                    ? preconditionTaskTree.task
                    : null;
        }
        if (preconditionTaskTree.allOf != null && !preconditionTaskTree.allOf.isEmpty()) {
            return findTaskByType(preconditionTaskTree.allOf, targetType);
        }
        if (preconditionTaskTree.oneOf != null && !preconditionTaskTree.oneOf.isEmpty()) {
            return findTaskByType(preconditionTaskTree.oneOf, targetType);
        }
        return null;
    }
    private SessionTaskSummary findTaskByType(Set<RequestSession.SessionTaskTree> tree, String targetType) {

        for (SessionTaskTree currentNode : tree) {
            var found = findTaskByType(currentNode, targetType);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private SessionTaskSummary findTaskByName(SessionTaskTree taskTree, String taskName) {
        if (taskTree == null) {
            return null;
        }
        if (taskTree.task != null) {
            return Objects.equals(taskName, taskTree.task.name)
                    ? taskTree.task
                    : null;
        }
        if (taskTree.allOf != null && !taskTree.allOf.isEmpty()) {
            return findTaskByName(taskTree.allOf, taskName);
        }
        if (taskTree.oneOf != null && !taskTree.oneOf.isEmpty()) {
            return findTaskByName(taskTree.oneOf, taskName);
        }
        return null;
    }

    private SessionTaskSummary findTaskByName(Set<RequestSession.SessionTaskTree> tree, String taskName) {
        for (SessionTaskTree currentNode : tree) {
            var found = findTaskByName(currentNode, taskName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void doOid4VP(String profile, String taskName, boolean enrollInTrain) throws InterruptedException, InvalidJwtException, MalformedClaimException {
        var state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

        SessionTaskSummary oid4vpTask = findTaskByName(state, taskName);

        OID4VPTaskStart oid4VpTaskStart = new OID4VPTaskStart();
        oid4VpTaskStart.walletBaseUri = config.holder().url().toString();

        if (oid4vpTask != null) {
            this.requestSubmission.executeOid4VpTask(
                    this.personManagement.currentRequestor(),
                    this.requestManagement.currentRequest(),
                    oid4vpTask,
                    oid4VpTaskStart,
                    enrollInTrain
            );
        }

        int attempts = 0;
        do {
            Thread.sleep(1000);
            state = this.requestSubmission.fetchSummary(requestManagement.currentRequest());

            oid4vpTask = findTaskByName(state, taskName);
        } while(oid4vpTask != null && (++attempts) < 10);
        assertThat("Not a valid verifiable presentation", oid4vpTask, nullValue());
    }
}
