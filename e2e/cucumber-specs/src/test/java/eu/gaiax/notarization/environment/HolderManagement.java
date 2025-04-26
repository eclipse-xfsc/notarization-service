
package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.domain.AIP_1_0_Credential;
import eu.gaiax.notarization.domain.AIP_1_0_Results;
import eu.gaiax.notarization.domain.Credential;
import eu.gaiax.notarization.domain.Results;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import static io.restassured.RestAssured.UNDEFINED_PORT;
import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import io.restassured.response.ResponseBodyExtractionOptions;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
public class HolderManagement {

    private static final Logger logger = Logger.getLogger(HolderManagement.class);

    @Inject
    Configuration config;

    @Inject
    ProofGenerator proofGenerator;

    private String did;
    private String didMethod;

    @Before
    public void beforeEach(Scenario scenario) {
        if (did == null) {
            usePublicDid();
        }
    }

    public void usePublicDid() {
        try {
            var privateDid = createPrivateDid("sov");
            var rawDid = privateDid.path("result.did").toString();
            var rawVerKey = privateDid.path("result.verkey").toString();

            registerAtLedger(rawDid, rawVerKey);
            assignToPublicDid(rawDid);

            did = String.format("did:sov:%s", rawDid);
            didMethod = "sov";
            logger.infof("Using public Holder DID: %s", did);
        } catch(Throwable t) {
            logger.error("Could not create DID", t);
        }
    }

    public void usePrivateDid() {
        try {
            did = createPrivateDid()
                    .path("result.did")
                    .toString();
            didMethod = "key";
            logger.infof("Using private Holder DID: %s", did);
        } catch(Throwable t) {
            logger.error("Could not create DID", t);
        }
    }

    public void prepareDidForProof(String didMethod) throws ExecutionException, InterruptedException {
        this.did = proofGenerator.getDid(didMethod);
    }

    private ResponseBodyExtractionOptions createPrivateDid() {
        return createPrivateDid("key");
    }

    private ResponseBodyExtractionOptions createPrivateDid(String didMethod) {
        try {
            return given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(String.format("""
                              {
                                "method": "%s",
                                "options": {
                                  "key_type": "ed25519"
                                }
                              }
                          """, didMethod))
                    .post(config.holder().url().toString() + "/wallet/did/create")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body();
        } catch(Throwable t) {
            logger.error("Could not create private DID", t);
            throw new RuntimeException("Unable to create private DID.", t);
        }
    }

    private void assignToPublicDid(String did) {
        try {
            given()
                    .accept(ContentType.JSON)
                    .post(config.holder().url().toString() + "/wallet/did/public?did=" + did)
                    .then()
                    .statusCode(200);
        } catch (Throwable t) {
            logger.error("Unable to assign DID as public.", t);
            throw new RuntimeException("Unable to assign DID as public.", t);
        }
    }

    private void registerAtLedger(String rawDid, String rawVerKey) {
        try {
            given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(String.format("""
                              {
                                "role": "ENDORSER",
                                "alias": null,
                                "did": "did:sov:%s",
                                "verkey": "%s"
                              }
                              """, rawDid, rawVerKey))
                    .post(config.ledger().url().toString() + "/register")
                    .then()
                    .statusCode(200);
        } catch(Throwable t) {
            logger.error("Unable to register public DID at ledger.", t);
            throw new RuntimeException("Unable to register public DID at ledger.", t);
        }
    }

    public ValidatableResponse registerAtIndico(String rawDid, String rawVerKey) {
        RestAssured.port = 443;
        var resp = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(String.format("""
                              {
                                "network": "testnet",
                                "did": "%s",
                                "verkey": "%s"
                              }
                              """, rawDid, rawVerKey))
                .post("https://selfserve.indiciotech.io/nym")
                .then()
                .statusCode(200);
        RestAssured.port = UNDEFINED_PORT;
        return resp;
    }

    public String did() {
        return did;
    }

    public String didMethod() {
        return didMethod;
    }

    public String createInvitation() {
        var invite = given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .queryParam("auto_accept", "true")
                    .post(config.holder().url().toString() + "/connections/create-invitation")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().path("invitation_url").toString();
        return invite;
    }

    public Optional<Credential> fetchCredentialByToken(String requestToken){
        var resultCreds = given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .post(config.holder().url().toString() + "/credentials/w3c")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().as(Results.class);

        return resultCreds.results.stream()
            .filter(cred -> cred.requestToken().equals(requestToken))
            .findFirst();
    }

    public Optional<AIP_1_0_Credential> fetchAIP_1_0_CredentialByToken(String requestToken){
        var resultCreds = given()
                    .accept(ContentType.JSON)
                    .get(config.holder().url().toString() + "/credentials")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().as(AIP_1_0_Results.class);

        return resultCreds.results.stream()
            .filter(cred -> cred.requestToken().equals(requestToken))
            .findFirst();
    }

    public void removeCredential(String credentialId) {
        given()
                .pathParam("credentialId", credentialId)
                .delete(config.holder().url().toString() + "/credential/{credentialId}")
                .then()
                .statusCode(200);
    }
}
