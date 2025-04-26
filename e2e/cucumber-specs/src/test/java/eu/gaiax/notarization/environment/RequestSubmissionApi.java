package eu.gaiax.notarization.environment;

import com.fasterxml.jackson.databind.JsonNode;
import eu.gaiax.notarization.domain.*;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.quarkus.logging.Log;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.ValidatableResponse;

import java.security.Key;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import eu.gaiax.notarization.domain.RequestSession.RequestValues;
import eu.gaiax.notarization.domain.RequestSession.SessionTaskSummary;

import static io.restassured.RestAssured.given;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.Month;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UnresolvableKeyException;


/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
public class RequestSubmissionApi {

    @Inject
    Logger logger;

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    ProfileManagement profileManagement;

    @Inject
    RequestManagement requestManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;

    @Inject
    HolderManagement holderManagement;

    @Inject
    KeycloakManagement keycloakManagement;

    @Inject
    ComplianceManagement complianceManagement;

    @Inject
    VerifiableCredentialGenerator verifiableCredentialGenerator;

    @Inject
    TrainEnrollment trainEnrollment;

    static {
        RestAssured.defaultParser = Parser.JSON;
    }

    public ValidatableResponse createSession() {
        return createSession(UUID.randomUUID().toString(),
                personManagement.currentRequestor(),
                this.profileManagement.someProfileId());
    }

    public ValidatableResponse createSession(Profile profile) {
        return createSession(UUID.randomUUID().toString(),
                personManagement.currentRequestor(),
                profile);
    }

    public ValidatableResponse createSession(String name, Profile profile) {
        return createSession(name,
                personManagement.currentRequestor(),
                profile);
    }

    public ValidatableResponse createSession(String name) {
        return createSession(name,
                personManagement.currentRequestor(),
                this.profileManagement.someProfileId());
    }

    public ValidatableResponse createSession(String requestName, Person person, Profile profileId) {
        var rawResponse = person.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"profileId":"%s"}
                        """, profileId.id()))
                .when().post(configuration.notarization().url().toString() + "/api/v1/session")
                .then();

        var response = rawResponse.extract();
        var createdSession = new RequestSession(
                requestName,
                response.path("sessionId").toString(),
                response.path("token").toString(),
                response.header("Location"),
                person,
                profileId,
                new ArrayList<>());
        this.requestManagement.registerSession(requestName, createdSession);

        this.httpResponseManagement.lastResponse = rawResponse;

        return rawResponse;
    }

    public ValidatableResponse submitNotarizationRequest() {
        var request = requestManagement.currentRequest();
        return submitNotarizationRequest(request);
    }

    public ValidatableResponse submitNotarizationRequest(String name) {
        var request = requestManagement.request(name);
        return submitNotarizationRequest(request);
    }

    public ValidatableResponse submitNotarizationRequest(RequestSession request) {
        return submitNotarizationRequest(
                request.person(),
                request);
    }

    public ValidatableResponse submitNotarizationRequest(Person person, RequestSession request) {

        var did = holderManagement.did();
        var invitation = holderManagement.createInvitation();
        var requestToken = UUID.randomUUID().toString();
        requestManagement.storeToken(request.name(), requestToken);

        //we store our token as givenName to later be able to search for the credential
        return submitNotarizationRequest(
                person,
                request,
                String.format(
                        """
                        {
                            "data": {
                                    "id": "did:example:b34ca6cd37bbf23",
                                    "givenName": "%s",
                                    "familyName": "SMITH",
                                    "gender": "Male",
                                    "image": "data:image/png;base64,iVBORw0KGgo...kJggg==",
                                    "residentSince": "2015-01-01",
                                    "lprCategory": "C09",
                                    "lprNumber": "999-999-999",
                                    "commuterClassification": "C1",
                                    "birthCountry": "Bahamas",
                                    "birthDate": "1958-07-17"
                            },
                            "holder": "%s",
                            "invitation": "%s"
                        }
                        """,requestToken, did, invitation));
    }

    public ValidatableResponse submitNotarizationRequestAIP10(Person person, RequestSession request) {
        var did = holderManagement.did();
        var invitation = holderManagement.createInvitation();
        var birthdate = LocalDate.of(1993, Month.MARCH, 9).toString();
        var requestToken = UUID.randomUUID().toString();
        requestManagement.storeToken(request.name(), requestToken);

        return submitNotarizationRequest(
                person,
                request,
                String.format(
                        """
                        {
                            "data": {
                                "givenName" : "%s",
                                "familyName" : "Doe",
                                "birthDate" : "%s"
                            },
                            "holder": "%s",
                            "invitation": "%s"
                        }
                        """, requestToken, birthdate, did, invitation));
    }

    public ValidatableResponse submitNotarizationRequestWithoutInvitation(RequestSession request) {
        var did = holderManagement.did();
        var birthdate = LocalDate.of(1993, Month.MARCH, 9).toString();
        var requestToken = UUID.randomUUID().toString();
        requestManagement.storeToken(request.name(), requestToken);

        return submitNotarizationRequest(
                request.person(),
                request,
                String.format(
                        """
                        {
                            "data": {
                                "givenName" : "%s",
                                "familyName" : "Doe",
                                "birthDate" : "%s"
                            },
                            "holder": "%s",
                            "invitation": null
                        }
                        """,  requestToken, birthdate, did));
    }

    public ValidatableResponse submitNotarizationRequest(Person person, RequestSession request, String body) {
        var rawResponse = person.given()
                .contentType(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .body(body)
                .when()
                .post(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/submission")
                .then();

        if (rawResponse.extract().statusCode() == 201) {
            var requestId = rawResponse.extract().jsonPath().getString("id");
            requestManagement.registerSubmittedRequest(request.name(), requestId);
        }

        this.httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

    public void updateNotarizationRequest(RequestSession request) {
        var did = holderManagement.did();
        var invitation = holderManagement.createInvitation();

        var dataToUpdate = String.format("""
                        {
                            "data": {

                            },
                            "holder": "%s",
                            "invitation": "%s"
                        }
                        """, did, invitation);

        var rawResponse = request.person().given()
                .contentType(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .body(dataToUpdate)
                .when()
                .put(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/submission")
                .then();

        this.httpResponseManagement.lastResponse = rawResponse;
    }

    public void deleteNotarizationRequest(RequestSession request) {
        var rawResponse = request.person().given()
                .contentType(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .when()
                .delete(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/submission")
                .then();

        this.httpResponseManagement.lastResponse = rawResponse;
    }

    public RequestValues fetchSummary(RequestSession request) {
        return fetchSummary(request.person(), request);
    }

    public RequestValues fetchSummary(Person person, RequestSession request) {
        var rawResponse = person.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}")
                .then();
        rawResponse.statusCode(200);
        var state = rawResponse.extract().as(RequestValues.class);

        request.values().add(state);

        this.httpResponseManagement.lastResponse = rawResponse;

        return state;
    }

    public ValidatableResponse markDone(Person person, RequestSession request) {
        var rawResponse = person.given()
                .contentType(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .when()
                .post(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/submission/ready")
                .then();

        this.httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

    public List<InvitationUrlResponse> fetchInvitationUrl(Person person, RequestSession request, int timeout) {
        var rawResponse = fetchInvitationUrl(person, request);

        var waiting = 0;
        while (rawResponse.extract().statusCode() != 200 && waiting++ < timeout) {
            try {
                logger.infov("Wait until invitation URL is available...");
                Thread.sleep(1000);
                rawResponse = fetchInvitationUrl(person, request);
            } catch (InterruptedException ex) { }
        }

        return rawResponse.extract().jsonPath().getList("$", InvitationUrlResponse.class);
    }

    public void performBrowserIdentificationTask(Person person, RequestSession request, SessionTaskSummary task) {

        logger.infov("Starting task");
        var response = startTask(person, request, task);

        String accessToken = this.keycloakManagement.getBearerToken(person);

        logger.warnv("Calling identification login {0}", accessToken);
        var startLoginResp = given()
                .redirects().follow(false)
                .header("Authorization", "Bearer " + accessToken)
                .when().get(response.uri)
                .then()
                .statusCode(303)
                .extract();

        String loginLocation = startLoginResp.header("Location");

        logger.warn("Following up login with access token");
        given()
            .redirects().follow(false)
            .header("Authorization", "Bearer " + accessToken)
            .cookies(startLoginResp.cookies())
            .when()
            .get(loginLocation)
            .then()
            .statusCode(303);
    }

    public void performVcIdentificationTask(Person person, RequestSession request, SessionTaskSummary task, VcTaskStart taskStart) {
        var rawResponse = startTaskSpec(person, request, task)
                .body(taskStart)
                .post(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/task")
                .then();

        this.httpResponseManagement.lastResponse = rawResponse;
    }

    public void executeComplianceCheckTask(Person person, RequestSession request, SessionTaskSummary task) {
        this.httpResponseManagement.lastResponse = startTaskSpec(person, request, task)
                .post(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/task")
                .then();

        var complianceSubmitUrl = this.httpResponseManagement.lastResponse.extract().jsonPath().getString("uri");
        complianceManagement.sendVPForComplianceCheck(complianceSubmitUrl);
    }

    public void executeOid4VpTask(Person person, RequestSession request, SessionTaskSummary task, OID4VPTaskStart oid4VPTaskStart, boolean enrollInTrain)
            throws InvalidJwtException {
        this.httpResponseManagement.lastResponse = startTaskSpec(person, request, task)
                .body(oid4VPTaskStart)
                .post(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/task")
                .then();

        var oid4VpWalletUrl = this.httpResponseManagement.lastResponse.extract().jsonPath().getString("uri");
        var parameters = new QueryStringDecoder(oid4VpWalletUrl).parameters();

        if (parameters.containsKey("request_uri") && parameters.containsKey("client_id")) {
            var requestAuthObjUri = parameters.get("request_uri").get(0);
            var clientId = parameters.get("client_id").get(0);

            var requestObj = given()
                .accept("application/oauth-authz-req+jwt")
                .get(requestAuthObjUri)
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

            // validate JWT
            var jwtConsumer = new JwtConsumerBuilder()
                    .setEnableRequireIntegrity()
                    .setRequireExpirationTime()
                    .setExpectedIssuer(clientId)
                    .setExpectedAudience("https://self-issued.me/v2")
                    .setVerificationKeyResolver((jws, list) -> {
                        try {
                            var certificate = jws.getCertificateChainHeaderValue().stream().findFirst();

                            if (certificate.isPresent()) {
                                var found = certificate.get().getSubjectAlternativeNames().stream()
                                        .filter(it -> it.get(1).toString().equals(clientId)).findAny();
                                if (found.isEmpty()) {
                                    Log.errorf("Unable to find a matching subject alternative name for clientID: %s", clientId);
                                }
                                return certificate.get().getPublicKey();
                            } else {
                                throw new RuntimeException("Unable to find a matching public key.");
                            }
                        } catch (JoseException | CertificateParsingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .build();

            var jwt = jwtConsumer.process(requestObj);
            var responseUri = jwt.getJwtClaims().getClaimValueAsString("response_uri");
            var state = jwt.getJwtClaims().getClaimValueAsString("state");
            var nonce = jwt.getJwtClaims().getClaimValueAsString("nonce");

            String verifiablePresentation;
            if (enrollInTrain) {
                verifiablePresentation = verifiableCredentialGenerator.getVerifiablePresentation(nonce, true);
                var latestUsedDid = verifiableCredentialGenerator.getWebDid();
                trainEnrollment.doTrainEnrollmentDirectly(latestUsedDid);
            } else {
                verifiablePresentation = verifiableCredentialGenerator.getVerifiablePresentation(nonce, false);
            }

            given()
                .contentType(ContentType.URLENC)
                .formParam("vp_token", verifiablePresentation)
                .formParam("presentation_submission", """
                        {
                            "id": "Presentation Submission",
                            "definition_id": "8f0f3a49-f710-4503-8882-7f60daa07218",
                            "descriptor_map": [
                                {
                                    "id": "232111d2-f4ca-41b7-861d-0ae714520c45",
                                    "format": "ldp_vp",
                                    "path": "$",
                                    "path_nested": {
                                        "format": "ldp_vc",
                                        "path": "$.verifiableCredential[0]"
                                    }
                                }
                            ]
                        }
                        """)
                .formParam("state", state)
                .post(responseUri)
                .then()
                .statusCode(200);
        } else {
            throw new IllegalStateException("There are no query parameters 'request_uri' and 'client_id' provided.");
        }
    }

    private ValidatableResponse fetchInvitationUrl(Person person, RequestSession request) {
        return person.given()
                .contentType(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/submission/ssiInviteUrl")
                .then();
    }

    private BeginTaskResponse startTask(Person person, RequestSession request, SessionTaskSummary task) {
        var rawResponse = startTaskSpec(person, request, task)
                .post(configuration.notarization().url().toString() + "/api/v1/session/{sessionId}/task")
                .then();

        this.httpResponseManagement.lastResponse = rawResponse;
        return rawResponse.extract().as(BeginTaskResponse.class);
    }

    private RequestSpecification startTaskSpec(Person person, RequestSession request, SessionTaskSummary task) {
        return person.given()
                .contentType(ContentType.JSON)
                .pathParam("sessionId", request.sessionId())
                .queryParam("taskId", task.taskId)
                .when();
    }
}
