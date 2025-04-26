/*
 *
 */
package eu.gaiax.notarization.environment;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.Instant;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class IssuerManagement {

    @Inject
    Configuration config;

    @Inject
    ProfileManagement profileManagement;

    private static final Logger LOG = LoggerFactory.getLogger(IssuerManagement.class);

    private static final String ISSUE_CREDENTIAL_TEMPLATE = """
                {
                    "connection_id" : "%s",
                    "filter": {
                      "ld_proof": {
                        "credential": {
                          "@context": [
                            "https://www.w3.org/2018/credentials/v1",
                            "https://w3id.org/citizenship/v1"
                          ],
                          "credentialSubject": {
                            "id": "%s",
                            "familyName": "SMITH",
                            "gender": "Male",
                            "givenName": "JOHN",
                            "type": [
                              "PermanentResident",
                              "Person"
                            ]
                          },
                          "description": "Government of Example Permanent Resident Card.",
                          "identifier": "83627465",
                          "issuanceDate": "%s",
                          "issuer": "%s",
                          "name": "Permanent Resident Card",
                          "type": [
                            "VerifiableCredential",
                            "PermanentResidentCard"
                          ]
                        },
                        "options": {
                          "proofType": "Ed25519Signature2018"
                        }
                      }
                    },
                    "trace": true
                }""";

    private final static int MAX_SECOND_TO_WAIT_FOR_CRED = 15;

    private String latestCredentialDid;

    public void createCredential(String profile) {
        var did = createDid();
        var invitation = createInvitation()
                .extract()
                .body()
                .jsonPath()
                .getJsonObject("invitation");

        var connectionId = receiveInvitation(invitation);

        issueCredential(connectionId, profile, did);
        latestCredentialDid = did;
    }

    public String latestCredentialDid() {
        return latestCredentialDid;
    }

    public void waitTilTheCredentialIsIssued(String did) {
        var credentials = getCredentialResults(did);

        var waiting = 0;
        while (credentials.isEmpty() && waiting < MAX_SECOND_TO_WAIT_FOR_CRED) {
            try {
                LOG.info("Wait until credential is available...");
                Thread.sleep(1000);
                credentials = getCredentialResults(did);
                waiting++;
            } catch (InterruptedException ex) { }
        }

        LOG.info("Credential is issued and available.");
    }

    public <T> T getCredential(String did) {
        return getCredentials(did).extract().jsonPath().getJsonObject("results[0]");
    }

    public String createInvitationUrl() {
        return createInvitation()
                .extract()
                .body()
                .jsonPath()
                .getString("invitation_url");
    }

    private <T> List<T> getCredentialResults(String did) {
        return getCredentials(did)
            .extract()
            .jsonPath().getList("results");
    }

    private ValidatableResponse getCredentials(String did) {
        var getCredentialBody = String.format("{ \"subject_ids\": [ \"%s\" ] }", did);

        return given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getCredentialBody)
            .post(config.holder().url().toString() + "/credentials/w3c")
            .then();
    }

    private String createDid() {
        return given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{ \"method\": \"key\", \"options\": { \"key_type\": \"ed25519\" } }")
            .post(config.holder().url().toString() + "/wallet/did/create")
            .then()
            .statusCode(200)
            .extract()
            .body().jsonPath().getString("result.did");
    }

    private ValidatableResponse createInvitation() {
        return given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{ }")
            .queryParam("auto_accept", "true")
            .post(config.holder().url().toString() + "/connections/create-invitation")
            .then()
            .statusCode(200);
    }

    private <T> String receiveInvitation(T invitation) {
        return given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(invitation)
            .queryParam("auto_accept", "true")
            .post(config.issuer().url().toString() + "/connections/receive-invitation")
            .then()
            .statusCode(200)
            .extract()
            .body().jsonPath().getString("connection_id");
    }

    private void issueCredential(String connectionId, String profile, String did) {
        var profileDid = profileManagement.fetchIssuingDIDForProfile(profile);

        var issueCredentialBody = String.format(ISSUE_CREDENTIAL_TEMPLATE,
                connectionId,
                did,
                Instant.now().toString(),
                profileDid);

        // sleep a bit to make sure connection is ready
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ex) { }

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(issueCredentialBody)
            .post(config.issuer().url().toString() + "/issue-credential-2.0/send")
            .then()
            .statusCode(200)
            .extract()
            .body().jsonPath().getString("connection_id");
    }

}
