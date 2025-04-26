package eu.gaiax.notarization.environment;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
public class KeycloakManagement {

    private static final Logger logger = Logger.getLogger(KeycloakManagement.class);

    static {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }
    @Inject
    Configuration config;

    @Inject
    HttpResponseManagement httpResponseManagement;

    Keycloak keycloak;

    Map<Person, User> users = new HashMap<>();

    public static final Supplier<User> RAND_USER_GEN = () -> new User("username" + UUID.randomUUID(), "password" + UUID.randomUUID());

    @Before
    public void before(Scenario scenario) {
        getKeycloak();
        deletesUsers();
    }

    private Keycloak getKeycloak() {
        if (keycloak == null) {
            try {
                final Configuration.Keycloak keycloakConfig = config.keycloak();
                var admin = keycloakConfig.admin();
                keycloak = KeycloakBuilder.builder()
                        .serverUrl(keycloakConfig.url().toString())
                        .realm(admin.realm())
                        .username(admin.username())
                        .password(admin.password())
                        .clientId(admin.clientId())
                        .resteasyClient(ResteasyClientBuilder.newBuilder().build())
                        .build();

                var requestorRealm = keycloak.realm(keycloakConfig.realm());
                if (requestorRealm == null) {
                    RealmRepresentation rr = new RealmRepresentation();
                    rr.setId(keycloakConfig.realm());
                    rr.setRealm(keycloakConfig.realm());
                    rr.setEnabled(true);

                    keycloak.realms().create(rr);
                }
            } catch (Throwable t) {
                logger.error("Could not initialize keycloak", t);
            }

        }
        return keycloak;
    }

    private void deletesUsers() {
        var realm = getKeycloak().realm(this.config.keycloak().realm());
        realm.users().list().stream()
                .map(user -> user.getId())
                .forEach(id -> realm.users().delete(id));
    }

    private RoleRepresentation getRole(String roleName) {
        var realm = getKeycloak().realm(this.config.keycloak().realm());

        try {
            var role = realm.roles().get(roleName);
            logger.infof("Role {0} already exists", roleName);
            return role.toRepresentation();
        } catch (NotFoundException ex) {
            logger.infof("Creating role {0}", roleName);

            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setClientRole(Boolean.TRUE);
            roleRepresentation.setName(roleName);
            roleRepresentation.setId(roleName);

            realm.roles().create(roleRepresentation);
            return realm.roles().get(roleName).toRepresentation();
        }
    }

    private void addRoles(Response userCreationResp, List<String> roles) {
        var realm = getKeycloak().realm(this.config.keycloak().realm());
        var userId = CreatedResponseUtil.getCreatedId(userCreationResp);

        var roleRepresentations = roles.stream()
                .map(this::getRole)
                .collect(Collectors.toList());

        realm.users().get(userId).roles().realmLevel().add(roleRepresentations);
    }

    private void createUser(User user, final List<String> roles) {
        String identifier = UUID.randomUUID().toString();
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(user.password);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.username);
        userRepresentation.setFirstName("firstName " + user.username + identifier);
        userRepresentation.setLastName("lastName " + user.username + identifier);
        userRepresentation.setEmail("email." + user.username + "@email.email");
        userRepresentation.setCredentials(Arrays.asList(credential));
        userRepresentation.setEnabled(true);
        userRepresentation.setRealmRoles(roles);
        userRepresentation
                .singleAttribute("birthdate", "29.01.1992");

        logger.infov("Creating user [user:{0}] [password:{1}]", user.username, user.password);

        var realm = getKeycloak().realm(this.config.keycloak().realm());
        var userCreationResp = realm.users().create(userRepresentation);

        addRoles(userCreationResp, roles);
    }

    private Optional<String> findUserId(String username) {
        var realm = getKeycloak().realm(this.config.keycloak().realm());
        return realm.users()
                .list()
                .stream()
                .filter(user -> user.getUsername().equals(username))
                .findAny()
                .map(user -> user.getId());
    }

    private void setEnabledForUser(String username, boolean enabled) {
        var realm = getKeycloak().realm(this.config.keycloak().realm());
        findUserId(username)
                .ifPresent(userId -> {
                    var user = realm.users().get(userId);
                    var userRepresentation = user.toRepresentation();

                    userRepresentation.setEnabled(enabled);
                    user.update(userRepresentation);
                });
    }

    public void lockUser(String username) {
        setEnabledForUser(username, Boolean.FALSE);
    }

    public void unlockUser(String username) {
        setEnabledForUser(username, Boolean.TRUE);
    }

    public void deleteUser(String username) {
        var realm = getKeycloak().realm(this.config.keycloak().realm());
        findUserId(username)
                .ifPresent(userId -> realm.users().delete(userId));
    }

    public String getBearerToken(Person person) {
        return getBearerToken(person, Arrays.asList());
    }

    public String getBearerToken(Person person, List<String> roles) {
        return getBearerToken(person, RAND_USER_GEN, roles);
    }

    public String getBearerToken(Person person, Supplier<User> userGenerator, List<String> roles) {
        logger.warnv("\nAquiring token\n", person);
        var foundUser = this.users.get(person);
        if (foundUser == null) {
            foundUser = userGenerator.get();
            createUser(foundUser, roles);
            this.users.put(person, foundUser);
        }
        return getBearerToken(foundUser, roles);
    }

    public void makeSureTrainEnrollmentUserExists() {
        createUser(new User("train-enrollment", "train-enrollment-secret"), List.of("enrolltf"));
    }

    public String getBearerToken(User user, List<String> roles) {
        return getBearerToken(user, roles, config.keycloak().realm(), config.keycloak().clientId(), config.keycloak().clientSecret());
    }

    public String getTrainBearerToken(Person person) {
        return getBearerToken(person, RAND_USER_GEN, List.of(config.keycloak().trainRole()));
    }

    public String getTrainBearerToken(Person person, Supplier<User> userGenerator, List<String> roles) {
        var foundUser = this.users.get(person);
        if (foundUser == null) {
            foundUser = userGenerator.get();
            createUser(foundUser, roles);
            this.users.put(person, foundUser);
        }
        return getBearerToken(foundUser, roles, config.keycloak().realm(), config.keycloak().trainClientId(), config.keycloak().trainClientSecret());
    }

    private String getBearerToken(User user, List<String> roles, String realm, String clientId, String clientSecret) {
        final var keycloakConfig = config.keycloak();

        var rawResponse = given()
                .pathParam("REALM", realm)
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("grant_type", "password")
                .formParam("username", user.username)
                .formParam("password", user.password)
                .post(keycloakConfig.url().toString() + "/realms/{REALM}/protocol/openid-connect/token")
                .then();

        this.httpResponseManagement.lastResponse = rawResponse;

        var accessToken = rawResponse
                .extract()
                .jsonPath().getString("access_token");

        return accessToken;
    }

    public static record User(String username, String password) {

    }
}
