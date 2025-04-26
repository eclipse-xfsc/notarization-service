package eu.gaiax.notarization.environment;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import eu.gaiax.notarization.domain.RequestSession;

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
public class RequestManagement {

    private RequestSession currentRequest;
    private Map<String, RequestSession> allRequests;
    private Map<String, String> submittedRequests;
    private Map<Person, List<RequestSession>> personalRequests;
    private Map<String, String> tokenizedRequests;

    @Before
    public void beforeEach(Scenario scenario) {
        this.currentRequest = null;
        this.allRequests = new HashMap<>();
        this.personalRequests = new HashMap<>();
        this.submittedRequests = new HashMap<>();
        this.tokenizedRequests = new HashMap<>();
    }

    public void registerSession(String requestName, RequestSession createdSession) {
        this.currentRequest = createdSession;
        this.allRequests.put(requestName, createdSession);
        var storedRequests = personalRequests.get(createdSession.person());
        if (storedRequests == null) {
            storedRequests = new ArrayList<>();
            personalRequests.put(createdSession.person(), storedRequests);
        }
        storedRequests.add(createdSession);
    }

    public void registerSubmittedRequest(String requestName, String requestId) {
        if (submittedRequests.containsKey(requestName)) {
            String errorMsg = String.format("There is already a submitted request with the name %s.", requestName);
            throw new IllegalStateException(errorMsg);
        } else {
            this.submittedRequests.put(requestName, requestId);
        }
    }

    public RequestSession currentRequest() {
        return currentRequest;
    }

    public RequestSession currentRequest(Person person) {
        var storedRequests = personalRequests.get(person);
        if (storedRequests == null) {
            throw new IllegalArgumentException("The given person does not have any requests");
        }
        if (storedRequests.isEmpty()) {
            throw new IllegalArgumentException("The given person does not have any requests");
        }
        return storedRequests.get(storedRequests.size() - 1);
    }

    public RequestSession request(String name) {
        var storedRequest = allRequests.get(name);
        if (storedRequest == null) {
            throw new IllegalArgumentException("No request could be found for the given name: " + name);
        }
        return storedRequest;
    }

    public String requestId(String requestName) {
        return submittedRequests.get(requestName);
    }

    public void storeToken(String requestName, String requestToken) {
        this.tokenizedRequests.put(requestName, requestToken);
    }
    public String requestToken(String requestName){
        return this.tokenizedRequests.get(requestName);
    }

}
