package eu.gaiax.notarization.environment;

import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import static io.restassured.RestAssured.given;


@ApplicationScoped
public class TSAServiceManagement {

    @Inject
    Configuration configuration;

    @Inject
    HttpResponseManagement httpResponseManagement;

    public void evaluateNotarizationRequest(JsonObject reqData, String policy) {
        httpResponseManagement.lastResponse = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(reqData)
                .post(configuration.tsa().url().toString() + "/policy/" + policy + "/evaluation")
                .then();
    }

}
