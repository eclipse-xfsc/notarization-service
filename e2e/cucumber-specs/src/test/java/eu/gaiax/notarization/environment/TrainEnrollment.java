package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.environment.notary.RequestListActionsApi;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.quarkus.oidc.client.Tokens;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import static io.restassured.RestAssured.UNDEFINED_PORT;
import static io.restassured.RestAssured.given;


@ApplicationScoped
public class TrainEnrollment {

    private static final String TRUSTLIST_FRAMEWORK_NAME = "notarization-api-tl";

    private static final String TRUSTLIST = """
            {
                "TrustServiceStatusList": {
                    "FrameworkInformation": {
                        "TSLVersionIdentifier": "1",
                        "TSLSequenceNumber": "1",
                        "TSLType": "http://TRAIN/TrstSvc/TrustedList/TSLType/federation1-POC",
                        "FrameworkOperatorName": {
                            "Name": "Federation 1"
                        },
                        "FrameworkOperatorAddress": {
                            "PostalAddresses": {
                                "PostalAddress": [
                                    {
                                        "StreetAddress": "Hauptsrasse",
                                        "Locality": "Stuttgart",
                                        "PostalCode": "70563",
                                        "CountryName": "DE"
                                    }
                                ]
                            },
                            "ElectronicAddress": {
                                "URI": "mailto:admin@federation1.de"
                            }
                        },
                        "FrameworkName": {
                            "Name": "%s"
                        },
                        "FrameworkInformationURI": {
                            "URI": "https://TRAIN/interoperability/federation-Directory"
                        },
                        "FrameworkAuditURI": {
                            "URI": "https://TRAIN/interoperability/Audit"
                        },
                        "FrameworkTypeCommunityRules": {
                            "URI": "https://TrustFramework_TRAIN.example.com/en/federation1-dir-rules.html"
                        },
                        "FrameworkScope": "EU",
                        "PolicyOrLegalNotice": {
                            "TSLLegalNotice": "The applicable legal framework for the present trusted list is   TBD. Valid legal notice text will be created."
                        },
                        "ListIssueDateTime": "2023-12-15T00:00:00Z"
                    }
                }
            }
            """;

    private static final String TRUST_SERVICE_PROVIDER = """
            {
                "TrustServiceProvider": {
                    "UUID": "%s",
                    "TSPName": "%s",
                    "TSPTradeName": "CompanyaA Gmbh",
                    "TSPInformation": {
                        "Address": {
                            "ElectronicAddress": "info@companya.de",
                            "PostalAddress": {
                                "City": "Stuttgart",
                                "Country": "DE",
                                "PostalCode": "11111",
                                "State": "BW",
                                "StreetAddress1": "Hauptsr",
                                "StreetAddress2": "071"
                            }
                        },
                        "TSPCertificationList": {
                            "TSPCertification": [
                                {
                                    "Type": "ISO:9001",
                                    "Value": "4356546745"
                                },
                                {
                                    "Type": "EU-VAT",
                                    "Value": "4356546745"
                                }
                            ]
                        },
                        "TSPEntityIdentifierList": {
                            "TSPEntityIdendifier": [
                                {
                                    "Type": "vLEI",
                                    "Value": "3453654764"
                                },
                                {
                                    "Type": "VAT",
                                    "Value": "3453654764"
                                }
                            ]
                        },
                        "TSPInformationURI": "string"
                    },
                    "TSPServices": {
                        "TSPService": [
                            {
                                "ServiceName": "Federation Notary",
                                "ServiceTypeIdentifier": "string",
                                "ServiceCurrentStatus": "string",
                                "StatusStartingTime": "string",
                                "ServiceDefinitionURI": "string",
                                "ServiceDigitalIdentity": {
                                    "DigitalId": {
                                        "X509Certificate": "sgdhfgsfhdsgfhsgfs",
                                        "DID": "%s"
                                    }
                                },
                                "AdditionalServiceInformation": {
                                    "ServiceBusinessRulesURI": "string",
                                    "ServiceGovernanceURI": "string",
                                    "ServiceIssuedCredentialTypes": {
                                        "CredentialType": [
                                            {
                                                "Type": "string"
                                            },
                                            {
                                                "Type": "string"
                                            }
                                        ]
                                    },
                                    "ServiceContractType": "string",
                                    "ServicePolicySet": "string",
                                    "ServiceSchemaURI": "string",
                                    "ServiceSupplyPoint": "string"
                                }
                            }
                        ]
                    }
                }
            }
            """;

    private static final String EXAMPLE_DID_ISSUER = "did:web:essif.iao.fraunhofer.de";

    private static final String TRAIN_ENROLLMENT_DATA = """
            {
                "tspJson": %s,
                "trustListEndpoint": null,
                "frameworkName": "%s"
            }
            """;

    private UUID trainEnrollmentCurrentTspId;

    private String trustFrameworkNameNonExistant;

    private Person trainEnrollmentAdmin;

    @Inject
    Configuration configuration;

    @Inject
    KeycloakManagement keycloakManagement;

    @Inject
    PersonManagement personManagement;
    @Inject
    RequestManagement requestManagement;
    @Inject
    HttpResponseManagement httpResponseManagement;

    @Inject
    Tokens tokens;

    private static final Logger LOG = LoggerFactory.getLogger(TrainEnrollment.class);

    @Before
    public void before(Scenario scenario) {
        if (trainEnrollmentAdmin == null) {
            this.trainEnrollmentAdmin = personManagement.iAmAnAdminForTRAINEnrollment();
        }

        try {
            if (! doesTrustListExist()) {
                var trustList = String.format(TRUSTLIST, TRUSTLIST_FRAMEWORK_NAME);
                trainEnrollmentAdmin.given()
                    .when()
                    .body(trustList)
                    .contentType(MediaType.APPLICATION_JSON)
                    .put(configuration.train().url().toString() + "/tspa-service/tspa/v1/init/json/" + TRUSTLIST_FRAMEWORK_NAME + "/trust-list")
                    .then()
                    .statusCode(201);
            }
        } catch (Exception ex) {
            LOG.warn("TRAIN service not running, skipping creation of Trust-List.");
        }
    }

    public void doTrainEnrollment(RequestListActionsApi.TaskInstance task) {
        doTrainEnrollment(task, TRUSTLIST_FRAMEWORK_NAME, EXAMPLE_DID_ISSUER);
    }

    public void doTrainEnrollmentNonExistantFramework(RequestListActionsApi.TaskInstance task) {
        doTrainEnrollment(task, trustFrameworkNameNonExistant, EXAMPLE_DID_ISSUER);
    }

    public void createTrustList(String frameworkName) {
        var trustList = String.format(TRUSTLIST, frameworkName);
        var accessToken = tokens.getAccessToken();

        RestAssured.port = 443;
        this.httpResponseManagement.lastResponse = given()
            .when()
            .body(trustList)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .put(configuration.trainExtern().url() + "/tspa-service/tspa/v1/init/json/" + configuration.trainExtern().frameworkName() + "/trust-list")
            .then();
        // ignore 400, because then Trust List already exists
        RestAssured.port = UNDEFINED_PORT;
    }

    public void doTrainEnrollmentDirectly(String issuer) {
        var tspId = UUID.randomUUID();
        var tspData = String.format(TRUST_SERVICE_PROVIDER, tspId, tspId, issuer);
        var accessToken = tokens.getAccessToken();

        RestAssured.port = 443;
        this.httpResponseManagement.lastResponse = given()
            .when()
            .body(tspData)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .put(configuration.trainExtern().url() + "/tspa-service/tspa/v1/" + configuration.trainExtern().frameworkName() + "/trust-list/tsp")
            .then()
            .statusCode(201);
        RestAssured.port = UNDEFINED_PORT;
    }

    public void doTrainEnrollment(RequestListActionsApi.TaskInstance task, String trustFrameworkName, String issuer) {
        var tspId = UUID.randomUUID();
        var tspData = String.format(TRUST_SERVICE_PROVIDER, tspId, tspId, issuer);
        var trainEnrollmentData = String.format(TRAIN_ENROLLMENT_DATA, tspData, trustFrameworkName);

        keycloakManagement.makeSureTrainEnrollmentUserExists();

        this.httpResponseManagement.lastResponse = trainEnrollmentAdmin.given()
                .when()
                .body(trainEnrollmentData)
                .contentType(MediaType.APPLICATION_JSON)
                .post(task.uri)
                .then();

        this.trainEnrollmentCurrentTspId = tspId;
    }

    public void prepareTrustFrameworkThatDoesNotExist() {
        this.trustFrameworkNameNonExistant = String.format("framework-%s", UUID.randomUUID());
    }

    public void assertTrustFrameworkDoesNotExist() {
        var trustListResponse = getTrustList(trustFrameworkNameNonExistant);
        trustListResponse.statusCode(404);
    }

    public void checkIfProviderEntryExists() {
        var trustListResponse = getTrustList(TRUSTLIST_FRAMEWORK_NAME);
        var doesContainTSP = trustListResponse
                .extract()
                .jsonPath()
                .getString("TrustServiceStatusList")
                .contains(trainEnrollmentCurrentTspId.toString());
        Assertions.assertTrue(doesContainTSP);
    }

    private boolean doesTrustListExist() {
        var trustListResponse = getTrustList(TRUSTLIST_FRAMEWORK_NAME);
        return trustListResponse.extract().statusCode() == 200;
    }

    private ValidatableResponse getTrustList(String trustFrameworkName) {
        return given()
            .when()
            .get(configuration.train().url().toString() + "/tspa-service/tspa/v1/" + trustFrameworkName + "/trust-list")
            .then();
    }
}
