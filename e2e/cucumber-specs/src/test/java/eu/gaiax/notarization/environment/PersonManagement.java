package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.domain.Profile;
import eu.gaiax.notarization.domain.RequestSession;
import eu.gaiax.notarization.domain.Role;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class PersonManagement {

    @Inject
    RequestManagement requestManagement;

    @Inject
    KeycloakManagement keycloakManagement;

    @Inject
    ProfileManagement profileManagement;

    private Person currentRequestor;
    private Person currentOperator;
    private Person currentAdmin;
    private String operatorProfile;

    private static final String ADMIN_ROLE = "admin";

    private static final String NOTARY_ROLE = "notary";


    public Person iAmARequestor() {
        this.currentRequestor = aRequestor();
        return this.currentRequestor;
    }

    public Person aRequestor() {
        var person = new PersonImpl(Role.Requestor);
        this.currentRequestor = person;
        return this.currentRequestor;
    }

    public Person iAmAnAdmin(String username, String password) {
        this.currentAdmin = new PersonImpl(Role.Admin);
        var user = new KeycloakManagement.User(username, password);

        var bearerToken = keycloakManagement.getBearerToken(currentAdmin, () -> user, List.of(ADMIN_ROLE, NOTARY_ROLE));
        var authToken = String.format("Bearer %s", bearerToken);
        currentAdmin.setAuthToken(authToken);

        return this.currentAdmin;
    }

    public Person iAmAnAdminForTRAINEnrollment() {
        this.currentAdmin = new PersonImpl(Role.Admin);

        var bearerToken = keycloakManagement.getTrainBearerToken(currentAdmin);
        var authToken = String.format("Bearer %s", bearerToken);
        currentAdmin.setAuthToken(authToken);

        return currentAdmin;
    }

    public Person iAmAnOperator(String profileId) {
        this.currentOperator = anOperator(profileId);
        this.operatorProfile = profileId;
        return this.currentOperator;
    }

    public Person iAmAnOperator(List<String> roles) {
        this.currentOperator = anOperator(roles);
        return this.currentOperator;
    }

    public Person anOperator(String username, String password, String profileId) {
        var roles = profileManagement.rolesOfProfile(new Profile(profileId));
        var person = new PersonImpl(Role.Notary);
        var user = new KeycloakManagement.User(username, password);
        var bearerToken = keycloakManagement.getBearerToken(person, () -> user, roles);
        this.operatorProfile = profileId;
        return anOpertor(person, bearerToken);
    }

    private Person anOperator(String profileId) {
        var roles = profileManagement.rolesOfProfile(new Profile(profileId));
        var person = new PersonImpl(Role.Notary);
        var bearerToken = keycloakManagement.getBearerToken(person, roles);

        this.operatorProfile = profileId;
        return anOpertor(person, bearerToken);
    }

    private Person anOperator(List<String> roles) {
        var person = new PersonImpl(Role.Notary);
        var bearerToken = keycloakManagement.getBearerToken(person, roles);

        return anOpertor(person, bearerToken);
    }
    private Person anOpertor(PersonImpl person, String bearerToken) {
        var authToken = String.format("Bearer %s", bearerToken);
        person.setAuthToken(authToken);

        this.currentOperator = person;

        return this.currentOperator;
    }

    @Before
    public void before(Scenario scenario) {
        currentRequestor = null;
        currentOperator = null;
        operatorProfile = null;
    }

    public Person currentRequestor() {
        if (currentRequestor == null) {
            this.currentRequestor = aRequestor();
        }
        return this.currentRequestor;
    }

    public Person currentOperator() {
        if (currentOperator == null) {
            // just an operator without profile or access token
            this.currentOperator = new PersonImpl(Role.Notary);
        }
        return this.currentOperator;
    }

    public Person currentAdmin() {
        return this.currentAdmin;
    }

    public String viewableProfile() {
        if (currentOperator == null || operatorProfile == null) {
            throw new IllegalStateException("You're not logged in as operator.");
        }
        return this.operatorProfile;
    }

    public void setProfileId(String profileId) {
        this.operatorProfile = profileId;
    }

    private class PersonImpl implements Person {

        private final Role role;
        private String authToken;

        private PersonImpl(Role role) {
            this(role, null);
        }

        private PersonImpl(Role role, String authToken) {
            this.role = role;
            this.authToken = authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        @Override
        public Role role() {
            return role;
        }

        @Override
        public RequestSpecification given() {
            var specBuilder = RestAssured.given();
            if (authToken != null) {
                specBuilder.header("Authorization", authToken);
            }
            else if (role == Role.Requestor) {
                RequestSession currentRequest;
                try {
                    currentRequest = requestManagement.currentRequest(this);
                } catch(IllegalArgumentException ex) {
                    currentRequest = null;
                }
                if (currentRequest != null && currentRequest.token() != null) {
                    specBuilder.header("token", String.format(currentRequest.token()));
                }
            }
            return specBuilder;
        }

    }

}
