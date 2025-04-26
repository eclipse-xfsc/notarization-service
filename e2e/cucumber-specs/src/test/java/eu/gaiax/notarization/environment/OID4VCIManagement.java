package eu.gaiax.notarization.environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.notarization.domain.CredentialOffer;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static io.restassured.RestAssured.given;

@ApplicationScoped
public class OID4VCIManagement {

    @Inject
    RequestSubmissionApi requestSubmission;

    @Inject
    PersonManagement personManagement;

    @Inject
    RequestManagement requestManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;

    private URI currentOid4vciOfferUrl;

    public void fetchOfferUrl() {
        var invitationUrls = requestSubmission.fetchInvitationUrl(
                personManagement.currentRequestor(),
                requestManagement.currentRequest(),
                10
        );

        var offerUrl = invitationUrls.stream()
                .filter(invitationUrl -> invitationUrl.issuerVersion.equals("V2"))
                .findFirst();

        Assertions.assertTrue(offerUrl.isPresent());
        this.currentOid4vciOfferUrl = offerUrl.get().inviteUrl;
    }

    public CredentialOffer extractCredentialOffer() throws JsonProcessingException {
        Map<String, List<String>> parameters = new QueryStringDecoder(currentOid4vciOfferUrl).parameters();

        if (parameters.containsKey("credential_offer")) {
            var credOfferRaw = parameters.get("credential_offer").get(0);
            var urlDecodedCredOffer = URLDecoder.decode(credOfferRaw, StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(urlDecodedCredOffer, CredentialOffer.class);
        } else {
            throw new RuntimeException("There is no credential offer available.");
        }
    }

    public String getCredentialEndpoint(CredentialOffer credOffer) {
        return given()
                .when()
                .get(credOffer.credential_issuer + ".well-known/openid-credential-issuer")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("credential_endpoint");
    }

    public String getTokenEndpoint(CredentialOffer credOffer) {
        return given()
                .when()
                .get(credOffer.credential_issuer + ".well-known/oauth-authorization-server")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token_endpoint");
    }

    public ValidatableResponse callTokenEndpoint(String tokenEndpoint, CredentialOffer credOffer) {
        var preAuthorizedCode =  credOffer.grants.get("urn:ietf:params:oauth:grant-type:pre-authorized_code").get("pre-authorized_code").asText();
        return given()
                .when()
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code")
                .formParam("pre-authorized_code", preAuthorizedCode)
                .post(tokenEndpoint)
                .then()
                .statusCode(200);
    }

    public void callCredentialEndpointWithLdpVpProof(String credentialEndpoint, String accessToken, String proof, CredentialOffer credOffer) {
        callCredentialEndpoint(credentialEndpoint, accessToken, proof, "ldp_vp", credOffer);
    }

    public void callCredentialEndpointWithJWTProof(String credentialEndpoint, String accessToken, String proof, CredentialOffer credOffer) {
        callCredentialEndpoint(credentialEndpoint, accessToken, proof, "jwt", credOffer);
    }

    public void callCredentialEndpoint(String credentialEndpoint, String accessToken, String proof, String proofType, CredentialOffer credOffer) {
        Assertions.assertFalse(credOffer.credential_configuration_ids.isEmpty());

        String requestProof;
        if (proofType.equals("jwt")) {
            requestProof = String.format("\"%s\"", proof);
        } else {
            requestProof = proof;
        }

        this.httpResponseManagement.lastResponse = given()
                .when()
                .contentType(ContentType.JSON)
                .header("Authorization", String.format("Bearer %s", accessToken))
                .body(String.format("""
                        {
                            "credential_identifier": "%s",
                            "proof": {
                                "proof_type": "%s",
                                "%s": %s
                            }
                        }
                      """, credOffer.credential_configuration_ids.get(0), proofType, proofType, requestProof))
                .post(credentialEndpoint)
                .then()
                .statusCode(200);
    }
}
