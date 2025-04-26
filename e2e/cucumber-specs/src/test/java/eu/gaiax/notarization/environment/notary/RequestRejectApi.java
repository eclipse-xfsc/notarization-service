
package eu.gaiax.notarization.environment.notary;

import eu.gaiax.notarization.environment.Configuration;
import eu.gaiax.notarization.environment.HttpResponseManagement;
import eu.gaiax.notarization.environment.PersonManagement;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class RequestRejectApi {

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;

    public ValidatableResponse rejectNotarizationRequest(String requestId, String profileId) {
        var rawResponse = personManagement
                .currentOperator()
                .given()
                .contentType(ContentType.JSON)
                .pathParam("notarizationRequestId", requestId)
                .pathParam("profileId", profileId)
                .when()
                .post(configuration.notarization().url().toString() + "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/reject")
                .then();

        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

}
