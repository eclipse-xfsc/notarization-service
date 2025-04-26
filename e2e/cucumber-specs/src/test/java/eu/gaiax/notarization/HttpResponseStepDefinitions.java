package eu.gaiax.notarization;

import eu.gaiax.notarization.environment.HttpResponseManagement;
import eu.gaiax.notarization.environment.PersonManagement;
import static io.restassured.RestAssured.given;
import io.cucumber.java.en.Then;
import io.restassured.http.ContentType;

import jakarta.inject.Inject;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Neil Crossley
 */
public class HttpResponseStepDefinitions {

    @Inject
    HttpResponseManagement responseManagement;

    @Inject
    PersonManagement personManagement;

    @Then("the response has the status code {int}")
    public void the_response_has_the_status_code(Integer statusCode) {
        responseManagement.lastResponse.statusCode(statusCode);
    }

    @Then("the response has an unaccessible location header without credentials")
    public void the_response_has_a_unauthorized_location_header_with_credentials() {
        responseManagement.lastResponse.header("Location", is(notNullValue()));
        var locationHeader = responseManagement.lastResponse.extract().header("Location");
        given().accept(ContentType.JSON).get(locationHeader).then().statusCode(
                allOf(greaterThanOrEqualTo(400), lessThan(500)));
    }

    @Then("the response has a resolvable location header")
    public void the_response_has_a_resolvable_location_header() {
        responseManagement.lastResponse.header("Location", is(notNullValue()));
        var locationHeader = responseManagement.lastResponse.extract().header("Location");
        given().accept(ContentType.JSON).get(locationHeader).then().statusCode(
                allOf(greaterThanOrEqualTo(200), lessThan(300)));
    }

    @Then("the response has the fields")
    public void the_response_has_the_fields(List<String> fields) {
        responseManagement.lastResponse.body("$", allOf(
                fields.stream()
                        .map(f -> hasKey(f))
                        .collect(Collectors.toList())));
    }

    @Then("the identity response has the fields")
    public void the_identity_response_has_the_fields(List<String> fields) {
        responseManagement.lastResponse.body("$", allOf(
                fields.stream()
                        .map(f -> everyItem(hasKey(f)))
                        .collect(Collectors.toList())));
    }

}
