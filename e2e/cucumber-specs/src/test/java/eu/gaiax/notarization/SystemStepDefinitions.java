package eu.gaiax.notarization;

import eu.gaiax.notarization.environment.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import java.util.List;


public class SystemStepDefinitions {

    @Inject
    ReadinessManagement readinessManagement;

    @Inject
    HttpResponseManagement responseManagement;

    @Inject
    AutoNotary autoNotary;

    @Inject
    TSAServiceManagement tsaServiceManagement;

    @Given("The {string} service is running")
    public void the_service_is_running(String serviceName) {
        readinessManagement.checkIfServiceIsRunning(serviceName);
    }

    @When("I perform a readiness check at the {string} service")
    public void i_perform_a_readiness_check_at_the_service(String serviceName) {
        readinessManagement.checkIfServiceIsReady(serviceName);
    }

    @Then("I receive a response that contains information about")
    public void i_receive_a_response_that_contains_information_about(List<String> healthCheckServiceNames) {
        var lastResponse = responseManagement.lastResponse;
        var listOfChecks = lastResponse
                .extract()
                .jsonPath()
                .getList("checks", JsonObject.class)
                .stream()
                .map(check -> check.getString("name"))
                .toList();
        Assertions.assertTrue(listOfChecks.containsAll(healthCheckServiceNames));
    }

    @When("the auto-notary is running and finds the request {string}")
    public void the_auto_notary_is_running_and_finds_the_request(String reqName) {
        autoNotary.findNotarizationRequest(reqName);
    }

    @Then("the request {string} is automatically evaluated based on the TSA policy {string}")
    public void the_request_is_automatically_evaluated_based_on_the_tsa_policy(String reqName, String policy) {
        var reqData = autoNotary.fetchAvailableRequestData(reqName);
        tsaServiceManagement.evaluateNotarizationRequest(reqData, policy);
    }

    @Then("the request {string} is automatically accepted")
    public void the_request_is_automatically_accepted(String reqName) {
        var tsaResponse = responseManagement.lastResponse;

        tsaResponse.statusCode(200);
        var validReq = tsaResponse.extract().body().jsonPath().getBoolean("allow");

        Assertions.assertTrue(validReq);

        autoNotary.acceptRequest(reqName);
    }
}
