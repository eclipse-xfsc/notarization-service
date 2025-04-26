
package eu.gaiax.notarization.environment.notary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.notarization.environment.Configuration;
import eu.gaiax.notarization.environment.HttpResponseManagement;
import eu.gaiax.notarization.environment.PersonManagement;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.List;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class RequestListActionsApi {

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    HttpResponseManagement httpResponseManagement;
    @Inject
    ObjectMapper objectMapper;

    public List<TaskInstance> listActions(String requestId, String profileId) {
        var rawResponse = personManagement
                .currentOperator()
                .given()
                .pathParam("notarizationRequestId", requestId)
                .pathParam("profileId", profileId)
                .contentType(ContentType.JSON)
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/actions")
                .then();

        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse.extract().body().jsonPath().getList(".", TaskInstance.class);
    }

    public static class TaskInstance {
        public String taskId;
        public URI uri;
        public String taskName;
    }
}
