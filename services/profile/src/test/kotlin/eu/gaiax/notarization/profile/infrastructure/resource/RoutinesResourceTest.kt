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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.profile.domain.entity.PersistantProfile
import eu.gaiax.notarization.profile.domain.entity.ProfileDid
import eu.gaiax.notarization.profile.infrastructure.MockOauth2ServerResource
import eu.gaiax.notarization.profile.infrastructure.config.ConfigBackedProfileService
import eu.gaiax.notarization.profile.infrastructure.config.MultiProfileTestProfile
import eu.gaiax.notarization.profile.infrastructure.rest.client.*
import io.quarkus.runtime.StartupEvent
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.infrastructure.Infrastructure
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.function.Supplier


/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@TestProfile(MultiProfileTestProfile::class)
@QuarkusTestResource(
    MockSsiIssuanceV1Resource::class
)
@QuarkusTestResource(
    MockSsiIssuanceV2Resource::class
)
@QuarkusTestResource(MockRevocationResource::class)
@QuarkusTestResource(MockOauth2ServerResource::class)
open class RoutinesResourceTest {
    @SsiIssuanceV1WireMock
    lateinit var mockSsiIssuance: WireMockServer
    @SsiIssuanceV2WireMock
    lateinit var mockSsiIssuanceV2: WireMockServer
    @Inject
    lateinit var supportedVersions: Set<ApiVersion>

    @RevocationWireMock
    lateinit var mockRevocation: WireMockServer

    @Inject
    lateinit var profileService: ConfigBackedProfileService

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @BeforeEach
    @RunOnVertxContext
    fun setUp(asserter: UniAsserter) {

        mockSsiIssuance.resetAll()
        mockSsiIssuanceV2.resetAll()
        mockRevocation.resetAll()
        asserter.assertThat ({ ->
            sessionFactory.withSession { _ ->
                ProfileDid.deleteAll()
            }
        }, { count ->
            // HACK: force proper Uni execution
            logger.info { "Cleared total dids: $count"}
        }).assertThat ({ ->
            sessionFactory.withSession { _ ->
                PersistantProfile.deleteAll()
            }
        }, { count ->
            // HACK: force proper Uni execution
            logger.info { "Cleared total profiles: $count"}
        }).execute ( Supplier {
            val uni = Uni.createFrom()
                .item { -> profileService.onStartup(StartupEvent()) }
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            uni
        } )

    }

    @Test
    @RunOnVertxContext
    fun hasPersistedDidsAfterTriggeringInitialization(asserter: UniAsserter) {
        stubSsiProfileInitialization(this.mockSsiIssuance)
        stubSsi2ProfileInitialization(this.mockSsiIssuanceV2)
        stubRevocationProfileInitializationSuccess()
        RestAssured.given()
            .`when`()
            .post(INIT_PROFILES_PATH)
            .then()
            .statusCode(200)
            .body("totalChanges", Matchers.`is`(MultiProfileTestProfile.values.size))

        val totalAnon = MultiProfileTestProfile.values.count { it.kind == CredentialKind.AnonCred }
        val totalJsonLd = MultiProfileTestProfile.values.count { it.kind == CredentialKind.JsonLD }
        val totalSdJwt = MultiProfileTestProfile.values.count { it.kind == CredentialKind.SD_JWT }

        asserter.assertEquals(
            { sessionFactory.withSession { _ -> ProfileDid.count() } },
            (totalAnon + totalJsonLd * supportedVersions.size + totalSdJwt).toLong()
        )
    }

    @Test
    fun verifyRevocationInitializedAfterTriggeringInitialization() {
        stubSsiProfileInitialization(this.mockSsiIssuance)
        stubSsi2ProfileInitialization(this.mockSsiIssuanceV2)
        stubRevocationProfileInitializationSuccess()
        RestAssured.given()
            .`when`()
            .post(INIT_PROFILES_PATH)
            .then()
            .statusCode(200)
            .body("totalChanges", Matchers.`is`(MultiProfileTestProfile.values.size))
        mockRevocation.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo("/management/lists")).withQueryParam(
                "profile", equalTo(
                    MultiProfileTestProfile.values[0].id
                )
            )
        )
        mockRevocation.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo("/management/lists")).withQueryParam(
                "profile", equalTo(
                    MultiProfileTestProfile.values[1].id
                )
            )
        )
    }

    @Test
    fun alreadyInitializedRevocationListDoesNotPreventInitialization() {
        stubSsiProfileInitialization(this.mockSsiIssuance)
        stubSsi2ProfileInitialization(this.mockSsiIssuanceV2)
        stubRevocationProfileInitializationAlreadyCreated(MultiProfileTestProfile.values[0].id)
        stubRevocationProfileInitializationSuccess()
        RestAssured.given()
            .`when`()
            .post(INIT_PROFILES_PATH)
            .then()
            .statusCode(200)
            .body("totalChanges", Matchers.`is`(MultiProfileTestProfile.values.size))
    }

    @Test
    fun canProvideDidsAfterTriggeringInitialization() {
        val givenIssuingDid = "abcd:def:ghi-issuing"
        val givenRevocatingDid = "abcg:def:ghi-revocating"
        stubSsiProfileInitialization(mockSsiIssuance, givenIssuingDid, givenRevocatingDid)
        stubSsi2ProfileInitialization(mockSsiIssuanceV2, givenIssuingDid, givenRevocatingDid)
        stubRevocationProfileInitializationSuccess()
        RestAssured.given()
            .`when`()
            .post(INIT_PROFILES_PATH)
            .then()
            .statusCode(200)
            .body("totalChanges", Matchers.`is`(MultiProfileTestProfile.values.size))
        val resultResponse = RestAssured.given()
            .accept(ContentType.JSON)
            .pathParam(PROFILE_VARIABLE, MultiProfileTestProfile.values[0].id)
            .`when`()[DID_PATH_V1]
            .then()
            .statusCode(200)
            .body("issuingDid", Matchers.`is`(givenIssuingDid))
            .body("revocatingDid", Matchers.`is`(givenRevocatingDid))
            .extract()
    }

    @Test
    fun givenUninitializedProfileThenDidIsMissing() {
        RestAssured.given()
            .accept(ContentType.JSON)
            .pathParam(PROFILE_VARIABLE, MultiProfileTestProfile.values[0].id)
            .`when`()[DID_PATH_V1]
            .then()
            .statusCode(404)
    }

    @Test
    fun failureRevocationListDoesNotPreventInitialization() {
        stubSsiProfileInitialization(mockSsiIssuance)
        stubSsi2ProfileInitialization(mockSsiIssuanceV2)
        stubRevocationProfileInitializationStatus(MultiProfileTestProfile.values[0].id, 400)
        stubRevocationProfileInitializationSuccess()
        RestAssured.given()
            .`when`()
            .post(INIT_PROFILES_PATH)
            .then()
            .statusCode(200)
            .body("totalChanges", Matchers.`is`(MultiProfileTestProfile.values.size - 1))
    }

    @Test
    fun failureDidInitializatoinesNotPreventInitialization() {
        stubSsiProfileInitialization(mockSsiIssuance, MultiProfileTestProfile.values[0].id, 400)
        stubSsi2ProfileInitialization(mockSsiIssuanceV2, MultiProfileTestProfile.values[0].id, 400)
        stubSsiProfileInitialization(this.mockSsiIssuance)
        stubSsi2ProfileInitialization(this.mockSsiIssuanceV2)
        stubRevocationProfileInitializationSuccess()
        RestAssured.given()
            .`when`()
            .post(INIT_PROFILES_PATH)
            .then()
            .statusCode(200)
            .body("totalChanges", Matchers.`is`(MultiProfileTestProfile.values.size - 1))
    }

    private fun stubSsiProfileInitialization(wiremockInstance: WireMockServer) {
        wiremockInstance.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/profile/init"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody("""
                                  {
                                          "issuingDid": "abc:def:ghi-issuing",
                                          "revocatingDid": "abc:def:ghi-revocating"
                                  }
                                  """)
                ).atPriority(100)
        )
    }

    private fun stubSsi2ProfileInitialization(wiremockInstance: WireMockServer) {
        wiremockInstance.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/api/v2/issuance/init-service"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody("""
                                  {
                                          "issuingDid": "abc:def:ghi-issuing",
                                          "signatureType": "Ed25519Signature2018",
                                          "revocatingDid": "abc:def:ghi-revocating"
                                  }
                                  """)
                ).atPriority(100)
        )
    }

    private fun stubSsiProfileInitialization(wiremockInstance: WireMockServer, profileId: String, status: Int) {
        wiremockInstance.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/profile/init"))
                .withRequestBody(WireMock.matchingJsonPath("$.profileID", WireMock.equalTo(profileId)))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody("""
                                  {
                                          "issuingDid": "abc:def:ghi-issuing",
                                          "revocatingDid": "abc:def:ghi-revocating"
                                  }
                                  """)
                ).atPriority(5)
        )
    }

    private fun stubSsi2ProfileInitialization(wiremockInstance: WireMockServer, profileId: String, status: Int) {
        wiremockInstance.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/api/v2/issuance/init-service"))
                .withRequestBody(WireMock.matchingJsonPath("$.profileID", WireMock.equalTo(profileId)))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody("""
                                  {
                                          "issuingDid": "abc:def:ghi-issuing",
                                          "signatureType": "Ed25519Signature2018",
                                          "revocatingDid": "abc:def:ghi-revocating"
                                  }
                                  """)
                ).atPriority(5)
        )
    }

    private fun stubSsiProfileInitialization(wiremockInstance: WireMockServer, givenIssuingDid: String, givenRevocatingDid: String) {
        wiremockInstance.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/profile/init"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                            String.format(
                                """
                                {
                                        "issuingDid": "%s",
                                        "revocatingDid": "%s"
                                }
                                """, givenIssuingDid, givenRevocatingDid))
                        )
                )
    }

    private fun stubSsi2ProfileInitialization(wiremockInstance: WireMockServer, givenIssuingDid: String, givenRevocatingDid: String) {
        wiremockInstance.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/api/v2/issuance/init-service"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                            String.format(
                                """
                                {
                                        "issuingDid": "%s",
                                        "signatureType": "Ed25519Signature2018",
                                        "revocatingDid": "%s"
                                }
                                """, givenIssuingDid, givenRevocatingDid))
                )
        )
    }

    private fun stubRevocationProfileInitializationSuccess() {
        mockRevocation.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/management/lists"))
                .withQueryParam("profile", WireMock.matching(".+"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                ).atPriority(100)
        )
    }

    private fun stubRevocationProfileInitializationAlreadyCreated(profileId: String) {
        mockRevocation.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/management/lists"))
                .withQueryParam("profile", WireMock.equalTo(profileId))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(409)
                ).atPriority(5)
        )
    }

    private fun stubRevocationProfileInitializationStatus(profileId: String, status: Int) {
        mockRevocation.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/management/lists"))
                .withQueryParam("profile", WireMock.equalTo(profileId))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(status)
                ).atPriority(1)
        )
    }

    companion object {
        var logger = Logger.getLogger(RoutinesResourceTest::class.java)
        private const val PROFILE_VARIABLE = "profileId"
        private const val DID_PATH_V1 = "/api/v1/profiles/{profileId}/ssi-data/v1"
        private const val DID_PATH_V2 = "/api/v1/profiles/{profileId}/ssi-data/v2"
        private const val INIT_PROFILES_PATH = "api/v1/routines/request-init-profiles"
    }
}
