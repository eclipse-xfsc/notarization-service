
package eu.gaiax.notarization.environment.notary;

import com.fasterxml.jackson.databind.JsonNode;
import eu.gaiax.notarization.environment.Configuration;
import eu.gaiax.notarization.environment.HttpResponseManagement;
import eu.gaiax.notarization.environment.PersonManagement;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
public class RequestRevokeApi {

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;

    public ValidatableResponse revokeCredential(JsonNode credential) {

        var rawResponse = personManagement
                .currentOperator()
                .given()
                .contentType(ContentType.JSON)
                .body(credential)
                .when()
                .post(configuration.notarization().url().toString() + "/api/v1/revoke")
                .then();

        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

}
