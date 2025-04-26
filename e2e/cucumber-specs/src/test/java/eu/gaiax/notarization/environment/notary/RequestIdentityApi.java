
package eu.gaiax.notarization.environment.notary;

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
public class RequestIdentityApi {

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;

    public ValidatableResponse fetchAssignedIdentity(String requestId, String profileId) {
        var rawResponse = personManagement
                .currentOperator()
                .given()
                .pathParam("notarizationRequestId", requestId)
                .pathParam("profileId", profileId)
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/identity")
                .then();

        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

}
