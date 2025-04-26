/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.gaiax.notarization.profile.infrastructure.resource

import com.fasterxml.jackson.databind.ObjectMapper
import ellog.uuid.UUID
import eu.gaiax.notarization.api.issuance.ProfileIssuanceSpec
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.*
import eu.gaiax.notarization.profile.domain.entity.PersistantProfile
import eu.gaiax.notarization.profile.infrastructure.InjectOauth2Controls
import eu.gaiax.notarization.profile.infrastructure.MockOauth2ServerResource
import eu.gaiax.notarization.profile.infrastructure.Notary
import eu.gaiax.notarization.profile.infrastructure.Outh2ServerControls
import eu.gaiax.notarization.profile.infrastructure.rest.dto.ProfileDidRequest
import eu.gaiax.notarization.profile.infrastructure.rest.dto.ProvidedDidRequest
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.quarkus.vertx.VertxContextSupport
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Vertx
import jakarta.inject.Inject
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.time.Period
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@QuarkusTestResource(MockOauth2ServerResource::class)
class ProtectedProfileResourceTest {

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @InjectOauth2Controls
    lateinit var oauth2Controls: Outh2ServerControls

    @Inject
    lateinit var vertx: Vertx

    @BeforeEach
    fun setup() {
        oauth2Controls.reset()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00023")
    fun canPutSimpleProfile() {
        val notary = someNotary()
        this.oauth2Controls.addNotary(notary)
        val identifier = UUID.createTimeV7().toString()
        val givenInput = simpleProfile(identifier)
        putProfileIntoSystem(givenInput, identifier, notary.token)

        fetchProfile(identifier)
            .then()
            .statusCode(200)
            .body("description", equalTo(givenInput.description))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00023")
    fun canPutLizzieProfile() {
        val notary = someNotary()
        this.oauth2Controls.addNotary(notary)
        val identifier = "lizzi-demo-sd-jwt-" + UUID.createTimeV7().toString()
        val description = "A SD-JWT demonstration for the Lizzi Wallet"
        val givenInput =
            """
                {
                    "id": "$identifier",
                    "kind": "JSON-LD",
                    "name": "lizzi-demo-sd-jwt",
                    "description": "$description",
                    "encryption": "ECDH-ES+A256KW",
                    "notaryRoles": [
                        "lissi-realm-role",
                        "lissi-client-role"
                    ],
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
                        "jwt": ""
                    },
                    "documentTemplate": null,
                    "taskDescriptions": [
                            {
                                "name": "Skidentity-eIDAS-Identification",
                                "description": "Identification via browser using eID means.",
                                "serviceName": "skidentity-ident",
                                "encryptAtRest": true
                            }
                    ],
                    "tasks": {
                    },
                    "preconditionTasks": {
                        "taskName" : "Skidentity-eIDAS-Identification"
                    },
                    "preIssuanceActions": {
                    },
                    "postIssuanceActions": [
                    ],
                    "actionDescriptions": [
                    ]
                }
            """.trimIndent()
        putProfileIntoSystem(givenInput, identifier, notary.token)

        fetchProfile(identifier)
            .then()
            .statusCode(200)
            .body("description", equalTo(description))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00023")
    fun canAssignV2DidToSimpleProfile() {
        val notary = someNotary()
        this.oauth2Controls.addNotary(notary)
        val identifier = UUID.createTimeV7().toString()

        putProfileIntoSystem(simpleProfile(identifier), identifier, notary.token)

        val didV2 = ProvidedDidRequest()
        didV2.v1 = null
        didV2.v2 = ProfileIssuanceSpec("did:something:1234", "did:something:4321", SignatureType.ECDSASECP256K1SIGNATURE2019)

        putProfileDidSystem(didV2, identifier, notary.token)
    }

    private fun simpleProfile(identifier: String): Profile {
        val givenInput = Profile(
            identifier,
            AipVersion.V2_0,
            CredentialKind.JsonLD,
            "some Name",
            "some description" + UUID.createTimeV7().toString(),
            setOf("major"),
            "A256GCM",
            listOf(),
            Period.ofYears(1),
            true,
            ObjectMapper().readTree(
                """
                        {"something":"other"}
                        """
            ),
            """
                { "evidenceDocument": "<documents:{doc|<doc.sha256.base64>}; separator=" ,">" }
                """,
            listOf(
                TaskDescription("name", TaskType.BROWSER_IDENTIFICATION_TASK, "desc"),
                TaskDescription("name2", TaskType.BROWSER_IDENTIFICATION_TASK, "desc")
            ),
            ProfileTaskTree(),
            ProfileTaskTree(),
            ProfileTaskTree(),
            listOf(),
            listOf()
        )
        return givenInput
    }

    private fun someNotary(): Notary {
        val notary = Notary(
            subject = UUID.createTimeV7().toString(),
            name = UUID.createTimeV7().toString(),
            roles = setOf("notary"),
            token = UUID.createTimeV7().toString(),
        )
        return notary
    }

    @Test
    fun canPutProfileMultipleTimes() {
        val notary = someNotary()
        this.oauth2Controls.addNotary(notary)
        val identifier = UUID.createTimeV7().toString()
        val givenInput = Profile(
            identifier,
            null,
            CredentialKind.JsonLD,
            "some Name",
            "some description",
            setOf("minor"),
            "A256GCM",
            listOf(),
            Period.ofYears(1),
            true,
            ObjectMapper().readTree(
                """
                        {"something":"other"}
                        """
            ),
            """
                { "evidenceDocument": "<documents:{doc|<doc.sha256.base64>}; separator=" ,">" }
                """,
            listOf(
                TaskDescription("name", TaskType.BROWSER_IDENTIFICATION_TASK, "desc"),
                TaskDescription("name2", TaskType.BROWSER_IDENTIFICATION_TASK, "desc")
            ),
            ProfileTaskTree(),
            ProfileTaskTree(),
            ProfileTaskTree(),
            listOf(),
            listOf()
        )

        val givenInput2 = givenInput.copy(description = "A 2. description ${UUID.createTimeV7()}")
        val givenInput3 = givenInput2.copy(description = "A 3. description ${UUID.createTimeV7()}")
        putProfileIntoSystem(givenInput, identifier, notary.token)
        putProfileIntoSystem(givenInput2, identifier, notary.token)
        putProfileIntoSystem(givenInput3, identifier, notary.token)

        fetchProfile(identifier)
            .then()
            .statusCode(200)
            .body("description", equalTo(givenInput3.description))
    }

    private fun fetchProfile(
        identifier: String
    ) = RestAssured.given()
        .contentType(ContentType.JSON)
        .`when`()
        .get("$PROFILE_PATH/$identifier")!!

    private fun putProfileIntoSystem(givenInput: Profile, identifier: String) {
        putProfileIntoSystem(givenInput, identifier, null)
    }
    private fun putProfileIntoSystem(givenInput: Profile, identifier: String, accesstoken: String?) {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(givenInput)
            .auth().oauth2(accesstoken)
            .`when`()
            .put("$PROTECTED_PROFILE_PATH/$identifier")
            .then()
            .statusCode(204)
    }

    private fun putProfileIntoSystem(givenInput: String, identifier: String, accesstoken: String?) {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(givenInput)
            .auth().oauth2(accesstoken)
            .`when`()
            .put("$PROTECTED_PROFILE_PATH/$identifier")
            .then()
            .statusCode(204)
    }

    private fun putProfileDidSystem(givenInput: ProfileDidRequest, identifier: String, accesstoken: String?) {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(givenInput)
            .auth().oauth2(accesstoken)
            .`when`()
            .post("$PROTECTED_PROFILE_PATH/$identifier/did")
            .then()
            .statusCode(204)
    }

    @Test
    fun canDeleteProfile() {
        val notary = someNotary()
        this.oauth2Controls.addNotary(notary)
        val givenProfileId = UUID.createTimeV7().toString()
        VertxContextSupport.subscribeAndAwait { ->
            sessionFactory.withTransaction { _ ->
                val givenInput = PersistantProfile()
                givenInput.id = UUID.createTimeV7().toString()
                givenInput.profileId = givenProfileId
                givenInput.capability = AipVersion.V2_0
                givenInput.description = "some description"
                givenInput.encryption = "A256GCM"
                givenInput.persistAndFlush() }
        }
        RestAssured.given()
            .`when`()
            .auth().oauth2(notary.token)
            .delete("$PROTECTED_PROFILE_PATH/$givenProfileId")
            .then()
            .statusCode(204)
        val item = VertxContextSupport.subscribeAndAwait { ->
            sessionFactory.withSession { PersistantProfile.findByProfileIdOptionally(givenProfileId) }
        }
        assertThat(item, `is`(nullValue()))
    }

    companion object {
        private const val PROTECTED_PROFILE_PATH = "api/v1/protected/profiles"
        private const val PROFILE_PATH = "api/v1/profiles"
    }
}
