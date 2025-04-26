
package eu.gaiax.notarization.environment.notary;

import eu.gaiax.notarization.domain.PagedNotarizationRequestSummary;
import eu.gaiax.notarization.environment.Configuration;
import eu.gaiax.notarization.environment.HttpResponseManagement;
import eu.gaiax.notarization.environment.PersonManagement;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class RequestFetchApi {

    @Inject
    Configuration configuration;

    @Inject
    PersonManagement personManagement;

    @Inject
    RequestDeleteApi requestDeleteApi;

    @Inject
    HttpResponseManagement httpResponseManagement;

    private List<PagedNotarizationRequestSummary.NotarizationRequestSummary> requests;

    public boolean containsRequestWithId(String requestId) {
        if (requests == null) {
            throw new IllegalStateException("Fetch requests before checking if a request is contained.");
        }
        return requests.stream()
                .anyMatch(request -> request.id().toString().equals(requestId));
    }

    public int amountOfRequests() {
        if (requests == null) {
            throw new IllegalStateException("Fetch requests before retrieving amount of requests.");
        }
        return requests.size();
    }

    public List<PagedNotarizationRequestSummary.NotarizationRequestSummary> fetchOwnClaimedRequests() {
        requests = RequestFetchApi.this.fetchAllAvailableRequests(0, 10, "ownClaimed");
        return requests;
    }

    public List<PagedNotarizationRequestSummary.NotarizationRequestSummary> fetchAllClaimedRequests() {
        requests = RequestFetchApi.this.fetchAllAvailableRequests(0, 10, "allClaimed");
        return requests;
    }

    public List<PagedNotarizationRequestSummary.NotarizationRequestSummary> fetchAllAvailableRequests() {
        requests = RequestFetchApi.this.fetchAllAvailableRequests(0, 10, "available");
        return requests;
    }

    public List<PagedNotarizationRequestSummary.NotarizationRequestSummary> fetchAllAvailableRequests(int offset, int limit, String requestFilter) {
        var resp = fetchRequests(offset, limit, requestFilter);

        ExtractableResponse<Response> extractedResponse = resp.extract();
        int statusCode = extractedResponse.statusCode();
        if (statusCode == 401) {
            return Collections.emptyList();
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("Fetch requests expected to succeed, but had the unexpected status code: " +
                statusCode + " and response " +
                extractedResponse.body().toString());
        }

        var requestSummary = extractedResponse.as(PagedNotarizationRequestSummary.class);

        if ((++offset) * limit < requestSummary.requestCount()) {
            return Stream.of(requestSummary.notarizationRequests(), RequestFetchApi.this.fetchAllAvailableRequests(offset, limit, requestFilter))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            return requestSummary.notarizationRequests();
        }
    }

    public ValidatableResponse fetchRequest(String requestId, String profileId) {
        var rawResponse = personManagement
                .currentOperator()
                .given()
                .given()
                .pathParam("notarizationRequestId", requestId)
                .pathParam("profileId", profileId)
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}")
                .then();

        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }

    public ValidatableResponse fetchRequests(int offset, int limit, String requestFilter) {
        var rawResponse = personManagement
                .currentOperator()
                .given()
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("filter", requestFilter)
                .when()
                .get(configuration.notarization().url().toString() + "/api/v1/requests")
                .then();

        httpResponseManagement.lastResponse = rawResponse;
        return rawResponse;
    }
}
