package eu.gaiax.notarization.environment;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

@ApplicationScoped
public class ReadinessManagement {

    @Inject
    Configuration config;

    @Inject
    HttpResponseManagement responseManagement;

    private Map<String, URL> serviceUrls;

    @Before
    public void before(Scenario scenario) {
        serviceUrls = new HashMap<>() {
            {
                put("request-processing", config.notarization().url());
                put("profile", config.profile().url());
            }
        };
    }

    public void checkIfServiceIsRunning(String serviceName) {
        var serviceUrl = serviceUrls.get(serviceName);

        responseManagement.lastResponse = given()
            .accept(ContentType.JSON)
            .when()
            .post(serviceUrl.toString() + "/q/health/live")
            .then();

        responseManagement.lastResponse.statusCode(200);
    }

    public void checkIfServiceIsReady(String serviceName) {
        var serviceUrl = serviceUrls.get(serviceName);

        responseManagement.lastResponse = given()
                .accept(ContentType.JSON)
                .when()
                .post(serviceUrl.toString() + "/q/health/ready")
                .then();

        responseManagement.lastResponse.statusCode(200);
    }

}
