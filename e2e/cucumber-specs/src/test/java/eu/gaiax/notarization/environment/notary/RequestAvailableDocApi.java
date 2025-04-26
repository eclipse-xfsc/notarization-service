/*
 *
 */
package eu.gaiax.notarization.environment.notary;

import eu.gaiax.notarization.domain.DocumentFull;
import eu.gaiax.notarization.environment.Configuration;
import eu.gaiax.notarization.environment.HttpResponseManagement;
import eu.gaiax.notarization.environment.PersonManagement;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class RequestAvailableDocApi {

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;

    private DocumentFull latestDoc;

    public DocumentFull getLatestDoc() {
        return latestDoc;
    }

    public ValidatableResponse fetchAvailableDoc(String requestId, String documentId, String profileId) {
        var rawResponse = personManagement
                .currentOperator()
                .given()
                .pathParam("notarizationRequestId", requestId)
                .pathParam("profileId", profileId)
                .pathParam("documentId", documentId)
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/document/{documentId}")
                .then();

        latestDoc = rawResponse.extract().as(DocumentFull.class);
        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

}
