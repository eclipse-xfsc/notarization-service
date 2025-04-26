package eu.gaiax.notarization.environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.notarization.domain.Profile;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.jboss.logging.Logger;

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
public class ProfileManagement {

    private static final Logger logger = Logger.getLogger(ProfileManagement.class);

	private static final String PROFILES_ALL_PATH = "/api/v1/profiles";
	private static final String PROFILE_SINGLE = PROFILES_ALL_PATH + "/{profileId}";
	private static final String PROFILES_INITIALIZE_PATH = "/api/v1/routines/request-init-profiles";
	private static final String PROFILES_SSI_DATA = PROFILES_ALL_PATH + "/{profileId}/ssi-data";
	private static final String PROFILES_SSI_DATA_V1 = PROFILES_ALL_PATH + "/{profileId}/ssi-data/v1";
	private static final String PROFILES_SSI_DATA_V2 = PROFILES_ALL_PATH + "/{profileId}/ssi-data/v2";
	private static final String PROTECTED_PROFILE_RESOURCE = "/api/v1/protected/profiles/{profileId}";

	private Map<Profile, JsonObject> knownProfiles;

	private static final String ANON_CRED_PROFILE_TEMPLATE = """
			{
			    "id": "%s",
			    "kind": "AnonCred",
			    "name": "%s",
			    "description": "Just an Example AnonCred profile",
			    "encryption": "ECDH-ES+A256KW",
			    "notaryRoles": [ %s ],
			    "notaries": [
			        {
			            "algorithm": "ECDH-ES+A256KW",
			            "key": {
			                "kty": "EC",
			                "use": "enc",
			                "crv": "P-384",
			                "kid": "CJVbr_T_rbgAWkP3TBqqbgTO2w27ZWfE_Gsky88odao",
			                "x": "s9zwNWhaFk1S_Pm4Ec05wztC5iZ6W1QPemcywckgKk2fXx8IBFC9vTc2x0LatSTC",
			                "y": "c9xoL-nBZmifO42GCWnU32uksdg1TF-YmldqxVf_apJ6Yi-bG_cAu0LP3yAqXijw",
			                "alg": "ECDH-ES+A256KW"
			            }
			        }
			    ],
			    "validFor": "P100Y",
			    "isRevocable": true,
			    "template": {
					"attributes": [
						"givenName",
						"familyName",
						"birthDate",
						"evidenceDocument"
					]
				},
			    "documentTemplate": null,
			    "taskDescriptions": [],
			    "tasks": {},
			    "preconditionTasks": {},
			    "preIssuanceActions": {},
			    "postIssuanceActions": [],
			    "actionDescriptions": []
			}
			""";

	private static final String JSON_LD_PROFILE_TEMPLATE = """
			{
			    "id": "%s",
			    "kind": "JSON-LD",
			    "name": "%s",
			    "description": "Just an Example JsonLD profile",
			    "encryption": "ECDH-ES+A256KW",
			    "notaryRoles": [ %s ],
			    "notaries": [
			        {
			            "algorithm": "ECDH-ES+A256KW",
			            "key": {
			                "kty": "EC",
			                "use": "enc",
			                "crv": "P-384",
			                "kid": "CJVbr_T_rbgAWkP3TBqqbgTO2w27ZWfE_Gsky88odao",
			                "x": "s9zwNWhaFk1S_Pm4Ec05wztC5iZ6W1QPemcywckgKk2fXx8IBFC9vTc2x0LatSTC",
			                "y": "c9xoL-nBZmifO42GCWnU32uksdg1TF-YmldqxVf_apJ6Yi-bG_cAu0LP3yAqXijw",
			                "alg": "ECDH-ES+A256KW"
			            }
			        }
			    ],
			    "validFor": "P100Y",
			    "isRevocable": true,
			    "template": {
			        "@context": [
			            "https://www.w3.org/2018/credentials/v1",
			            "https://w3id.org/citizenship/v1"
			        ],
			        "type": [
			            "VerifiableCredential",
			            "PermanentResidentCard"
			        ],
			        "credentialSubject": {
			            "type": "PermanentResident"
			        }
			    },
			    "documentTemplate": null,
			    "taskDescriptions": [],
			    "tasks": {},
			    "preconditionTasks": {},
			    "preIssuanceActions": {},
			    "postIssuanceActions": [],
			    "actionDescriptions": []
			}
			""";

	private static final String EBSI_JSON_LD_PROFILE_TEMPLATE = """
			{
			    "id": "%s",
			    "kind": "JSON-LD",
			    "name": "%s",
			    "description": "Just an EBSI JsonLD profile",
			    "encryption": "ECDH-ES+A256KW",
			    "notaryRoles": [ %s ],
			    "notaries": [
			        {
			            "algorithm": "ECDH-ES+A256KW",
			            "key": {
			                "kty": "EC",
			                "use": "enc",
			                "crv": "P-384",
			                "kid": "CJVbr_T_rbgAWkP3TBqqbgTO2w27ZWfE_Gsky88odao",
			                "x": "s9zwNWhaFk1S_Pm4Ec05wztC5iZ6W1QPemcywckgKk2fXx8IBFC9vTc2x0LatSTC",
			                "y": "c9xoL-nBZmifO42GCWnU32uksdg1TF-YmldqxVf_apJ6Yi-bG_cAu0LP3yAqXijw",
			                "alg": "ECDH-ES+A256KW"
			            }
			        }
			    ],
			    "validFor": "P100Y",
			    "isRevocable": true,
			    "template": {
			        "@context": [
			            "https://www.w3.org/ns/credentials/v2"
			        ],
			        "type": [
			            "VerifiableCredential",
			            "StudentID"
			        ],
			        "credentialSubject": {
			            "student": true
			        },
			        "credentialSchema": {
			            "id": "https://api-pilot.ebsi.eu/trusted-schemas-registry/v2/schemas/0x23039e6356ea6b703ce672e7cfac0b42765b150f63df78e2bd18ae785787f6a2",
			            "type": "FullJsonSchemaValidator2021"
			        }
			    },
			    "documentTemplate": null,
			    "taskDescriptions": [],
			    "tasks": {},
			    "preconditionTasks": {},
			    "preIssuanceActions": {},
			    "postIssuanceActions": [],
			    "actionDescriptions": []
			}
			""";

    private static final String JSON_WEBSIGNATURE_2020_JSON_LD_PROFILE_TEMPLATE = """
			{
			    "id": "%s",
			    "kind": "JSON-LD",
			    "name": "%s",
			    "description": "Just an JsonLD profile",
			    "encryption": "ECDH-ES+A256KW",
			    "notaryRoles": [ %s ],
			    "notaries": [
			        {
			            "algorithm": "ECDH-ES+A256KW",
			            "key": {
			                "kty": "EC",
			                "use": "enc",
			                "crv": "P-384",
			                "kid": "CJVbr_T_rbgAWkP3TBqqbgTO2w27ZWfE_Gsky88odao",
			                "x": "s9zwNWhaFk1S_Pm4Ec05wztC5iZ6W1QPemcywckgKk2fXx8IBFC9vTc2x0LatSTC",
			                "y": "c9xoL-nBZmifO42GCWnU32uksdg1TF-YmldqxVf_apJ6Yi-bG_cAu0LP3yAqXijw",
			                "alg": "ECDH-ES+A256KW"
			            }
			        }
			    ],
			    "validFor": "P100Y",
			    "isRevocable": true,
			    "template": {
			        "@context": [
			            "https://www.w3.org/ns/credentials/v2"
			        ],
			        "type": [
			            "VerifiableCredential",
			            "StudentID"
			        ],
			        "credentialSubject": {
			            "student": true
			        },
			        "credentialSchema": {
			            "id": "https://api-pilot.ebsi.eu/trusted-schemas-registry/v2/schemas/0x23039e6356ea6b703ce672e7cfac0b42765b150f63df78e2bd18ae785787f6a2",
			            "type": "FullJsonSchemaValidator2021"
			        }
			    },
			    "documentTemplate": null,
			    "taskDescriptions": [],
			    "tasks": {},
			    "preconditionTasks": {},
			    "preIssuanceActions": {},
			    "postIssuanceActions": [],
			    "actionDescriptions": []
			}
			""";

	private String profileToSubmit;
	private Profile profileToSubmitId;

	@Inject
	Configuration configuration;

	@Inject
	HttpResponseManagement responseManagement;
    @Inject
    ObjectMapper objectMapper;

	@Inject
	PersonManagement personManagement;

	@Before
	public void before(Scenario scenario) {
		if (knownProfiles == null) {
            try {
                given()
                    .when()
                    .post(configuration.profile().url().toString() + PROFILES_INITIALIZE_PATH)
                    .then()
                    .statusCode(200);
            } catch(Throwable t) {
                logger.error("Could not initialize the profile DIDs!", t);
            }
            try {
                var pageResults = given()
                    .accept(ContentType.JSON)
                    .queryParam("size", 100)
                    .when().get(configuration.profile().url().toString() + PROFILES_ALL_PATH)
                    .as(JsonObject.class);
                var profiles = pageResults.getJsonArray("items");

                knownProfiles = profiles.stream().map(JsonValue::asJsonObject).collect(Collectors.toMap(
                        p -> new Profile(p.getString("id")), p -> p));
            } catch(Throwable t) {
                logger.error("Could not fetch known profiles!", t);
            }
		}
		this.currentProfileId = null;
        this.profileToSubmit = null;
        this.profileToSubmitId = null;
	}

	private Profile currentProfileId;

    public String fetchIssuingDIDForProfile(String profile) {
        var rawResponse = given()
                .when()
                .accept(ContentType.JSON)
                .pathParam("profileId", profile)
                .get(configuration.profile().url().toString() + PROFILES_SSI_DATA_V1)
                .then()
                .statusCode(200);

        return rawResponse.extract().jsonPath().getString("issuingDid");
    }

	public Profile someProfileId() {
		return somePortalProfileId();
	}

	public Profile somePortalProfileId() {
		currentProfileId = new Profile(this.configuration.portalProfileId());
		return currentProfileId;
	}

	public Profile profileIdWithAutoNotarization() {
		currentProfileId = new Profile(this.configuration.profileIdAutoNotarization());
		return currentProfileId;
	}

	public Profile profileIdWithIdentificationPrecondition() {
		currentProfileId = new Profile(this.configuration.profileIdWithIdentificationPrecondition());
		return currentProfileId;
	}

	public Profile profileIdWithComplianceCheck() {
		currentProfileId = new Profile(this.configuration.profileIdWithComplianceCheck());
		return currentProfileId;
	}

    public Profile profileIdWithoutTasks() {
        if (currentProfileId == null) {
            currentProfileId = new Profile(this.configuration.profileIdWithoutTasks());
        }
        return currentProfileId;
    }

    public Profile profileIdAIP10() {
        currentProfileId = new Profile(this.configuration.profileIdAip10());
        return currentProfileId;
    }

	public void createIndyProfile(String profileId) {
        createSubmittableProfile(new Profile(profileId), ANON_CRED_PROFILE_TEMPLATE, profileId, Set.of(profileId));
	}

    public void createJSONLDProfile(String profileId) {
        createJSONLDProfile(profileId, Set.of(profileId));
    }

	public void createJSONLDProfile(String profileId, Set<String> roles) {
        createSubmittableProfile(new Profile(profileId), JSON_LD_PROFILE_TEMPLATE, profileId, roles);
	}

	public void createEBSIJSONLDProfile(String profileId) {
        createSubmittableProfile(new Profile(profileId), EBSI_JSON_LD_PROFILE_TEMPLATE, profileId, Set.of(profileId));
	}

    public void createAndSubmitJsonWebSignatureProfile(String profileId) {
        if (! doesProfileExist(profileId)) {
            createSubmittableProfile(new Profile(profileId), JSON_WEBSIGNATURE_2020_JSON_LD_PROFILE_TEMPLATE, profileId, Set.of(profileId));
            submitProfile();
            didInitializeForProfile();
        }
    }

    public void createSubmittableProfile(Profile profileId, String template, String name, Set<String> roles) {
        this.profileToSubmitId = profileId;
        var rawRoles = roles.stream()
            .map(role -> "\"" + role + "\"")
            .collect(Collectors.joining(", "));

        this.profileToSubmit = String.format(template, profileId, name, rawRoles);
    }

	public void submitProfile() {
		responseManagement.lastResponse = personManagement.currentAdmin().given()
				.when()
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.pathParam("profileId", profileToSubmitId.id())
				.body(profileToSubmit)
				.put(configuration.profile().url().toString() + PROTECTED_PROFILE_RESOURCE)
				.then()
				.statusCode(204);

		JsonObject currentProfile = Json.createReader(new StringReader(profileToSubmit)).readObject();

		this.knownProfiles.put(profileToSubmitId, currentProfile);
		currentProfileId = profileToSubmitId;
    }

    public void didInitializeForProfile() {
        responseManagement.lastResponse = personManagement.currentAdmin().given()
            .when()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .pathParam("profileId", profileToSubmitId.id())
            .body("""
                {
                    "type": "automatic",
                    "versions" : [
                        "v2"
                    ],
                    "keyType" : "Ed25519",
                    "signatureType" : "JsonWebSignature2020"
                }
                """)
            .post(configuration.profile().url().toString() + PROTECTED_PROFILE_RESOURCE + "/did")
            .then()
            .statusCode(204);
    }

	public void canFetchProfile(String profileId) {
		responseManagement.lastResponse = given()
				.when()
				.accept(ContentType.JSON)
				.pathParam("profileId", profileId)
				.get(configuration.profile().url().toString() + PROFILE_SINGLE)
				.then()
				.statusCode(200);

        var foundProfile = responseManagement.lastResponse.extract().as(JsonObject.class);
        var profile = new Profile(profileId);
        knownProfiles.put(profile, foundProfile);
        currentProfileId = profile;
	}

    public boolean doesProfileExist(String profileId) {
        return given()
            .when()
            .accept(ContentType.JSON)
            .pathParam("profileId", profileId)
            .get(configuration.profile().url().toString() + PROFILE_SINGLE)
            .then()
            .extract()
            .statusCode() == 200;
    }

    public List<String> rolesOfProfile(Profile profile) {
        var targetProfile = knownProfiles.get(profile);
        if (targetProfile == null) {
            logger.infov("Profile {0} is not known, falling back to the role with the same name.", profile);
            return List.of(profile.id());
        }
        return targetProfile.getJsonArray("notaryRoles").getValuesAs(JsonString::getString);
    }
}
