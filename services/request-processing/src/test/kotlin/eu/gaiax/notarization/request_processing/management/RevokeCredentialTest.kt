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
package eu.gaiax.notarization.request_processing.management

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import eu.gaiax.notarization.MockRevocationResource
import eu.gaiax.notarization.RevocationWireMock
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.ListMapping
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@Tag("security")
@QuarkusTestResource(MockRevocationResource::class)
class RevokeCredentialTest {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @RevocationWireMock
    lateinit var mockRevocation: WireMockServer

    private fun createLM(profileName: String, listName: String): ListMapping {
        val lm = ListMapping()
        lm.profileName = profileName
        lm.listName = listName
        return lm
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response, serviceName: String?) {
        logger.debug { " WireMock stub $serviceName request at URL: ${inRequest.absoluteUrl}" }
        logger.debug { " WireMock stub $serviceName request headers: \n${inRequest.headers}" }
        logger.debug { " WireMock stub $serviceName request body: \n${inRequest.bodyAsString}" }
        logger.debug { " WireMock stub $serviceName response body: \n${inResponse.bodyAsString}" }
        logger.debug { " WireMock stub $serviceName response headers: \n${inResponse.headers}" }
    }

    private fun stubConfigLists(listMapping: List<ListMapping>) {
        mockRevocation.resetAll()
        var body: String? = ""
        body = try {
            objectMapper.writeValueAsString(listMapping)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        mockRevocation.addMockServiceRequestListener { `in`: Request, out: Response ->
            requestReceived(
                `in`,
                out,
                "revocation"
            )
        }
        mockRevocation.stubFor(
            WireMock.get(WireMock.urlMatching(".*lists"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(body)
                        .withStatus(200)
                )
        )
        mockRevocation.stubFor(
            WireMock.delete(WireMock.urlMatching(".*entry/-.+"))
                .willReturn(WireMock.notFound())
        )
        mockRevocation.stubFor(
            WireMock.delete(WireMock.urlMatching(".*entry/[^-]+"))
                .willReturn(WireMock.noContent())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00015")
    fun profileNotFoundReturnsNotFound() {
        stubConfigLists(listOf())
        val givenNotary = MockState.notary1
        RestAssured.given()
            .header("Authorization", givenNotary.bearerValue())
            .header("Content-type", MediaType.APPLICATION_JSON)
            .body(
                """
                    {
                        "cred_value": {
                            "credentialStatus": {
                                "statusListIndex": "94567",
                                "statusListCredential": "https://revocation.example.com/status/unknownListName"
                            }
                        }
                    }
                """.trimIndent()
            )
            .`when`()
            .post(PATH)
            .then()
            .statusCode(404)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00015")
    fun foundProfileWithNoPermissionReturnsAuthenticateError() {
        stubConfigLists(
            listOf(
                createLM("aProfileNotOwnedByNotary", "noPermission")
            )
        )
        val givenNotary = MockState.notary1
        RestAssured.given()
            .header("Authorization", givenNotary.bearerValue())
            .header("Content-type", MediaType.APPLICATION_JSON)
            .body(
                """
                    {
                        "cred_value": {
                            "credentialStatus": {
                                "statusListIndex": "94567",
                                "statusListCredential": "https://revocation.example.com/status/noPermission"
                            }
                        }
                    }
                """.trimIndent()
            )
            .`when`()
            .post(PATH)
            .then()
            .statusCode(401)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00015")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00034")
    fun notaryWithAccessCanCallRevoke() {
        stubConfigLists(
            listOf(
                createLM(MockState.profile1.id, MockState.profile1.id + "_lst")
            )
        )
        val givenNotary = MockState.notary1
        RestAssured.given()
            .header("Authorization", givenNotary.bearerValue())
            .header("Content-type", MediaType.APPLICATION_JSON)
            .body(
                """
                    {
                        "cred_value": {
                            "credentialStatus": {
                                "statusListIndex": "94567",
                                "statusListCredential": "https://revocation.example.com/status/${MockState.profile1.id}_lst"
                            }
                        }
                    }
                """.trimIndent()
            )
            .`when`()
            .post(PATH)
            .then()
            .statusCode(204)
        mockRevocation.verify(
            WireMock.deleteRequestedFor(WireMock.urlMatching("/management/lists/" + MockState.profile1.id + "/entry/94567"))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00015")
    fun wrongIndexGetsNotFound() {
        stubConfigLists(
            java.util.List.of(
                createLM(MockState.profile1.id, MockState.profile1.id + "_lst")
            )
        )
        val givenNotary = MockState.notary1
        RestAssured.given()
            .header("Authorization", givenNotary.bearerValue())
            .header("Content-type", MediaType.APPLICATION_JSON)
            .body(
                """
                    {
                        "cred_value": {
                            "credentialStatus": {
                                "statusListIndex": "-1",
                                "statusListCredential": "https://revocation.example.com/status/${MockState.profile1.id}_lst"
                            }
                        }
                    }
                """.trimIndent()
            )
            .`when`()
            .post(PATH)
            .then()
            .statusCode(404)
    }

    companion object {
        const val PATH = "/api/v1/revoke"
    }
}
