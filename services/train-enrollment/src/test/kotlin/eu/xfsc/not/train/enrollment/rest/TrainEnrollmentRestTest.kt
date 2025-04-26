/****************************************************************************
 * Copyright 2024 ecsec GmbH
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
 ***************************************************************************/

package eu.xfsc.not.train.enrollment.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.UrlPattern
import eu.xfsc.not.train.enrollment.KeycloakRealmResourceManager
import eu.xfsc.not.train.enrollment.WireMockTrain
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.response.ResponseBodyExtractionOptions
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.net.MalformedURLException
import java.util.UUID


private val logger = KotlinLogging.logger {}


/**
 * @author Mike Prechtl
 */
@QuarkusTest
@QuarkusTestResource(WireMockTrain::class)
@QuarkusTestResource(KeycloakRealmResourceManager::class)
class TrainEnrollmentRestTest {

    companion object {
        val WIREMOCK_CALLBACK_SERVICE: WireMockServer =
            WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

        @JvmStatic
        @BeforeAll
        fun setup() {
            WIREMOCK_CALLBACK_SERVICE.addMockServiceRequestListener { `in`, out -> requestReceived(`in`, out) }
            WIREMOCK_CALLBACK_SERVICE.start()
        }

        @JvmStatic
        @AfterAll
        fun stop() {
            WIREMOCK_CALLBACK_SERVICE.stop()
        }

        @JvmStatic
        fun stubForCallbackService(urlPattern: String) {
            WIREMOCK_CALLBACK_SERVICE.stubFor(
                WireMock.post(WireMock.urlMatching(urlPattern))
                    .willReturn(WireMock.noContent())
            )
        }

        protected fun requestReceived(inRequest: Request, inResponse: Response) {
            logger.info { "WireMock callback stub request at URL: ${inRequest.absoluteUrl}" }
            logger.info { "WireMock callback stub request headers: \n${inRequest.headers}" }
            logger.info { "WireMock callback stub request body: \n${inRequest.bodyAsString}" }
        }

        fun verifySuccessfulCallbackCall() {
            WIREMOCK_CALLBACK_SERVICE.verify(1, RequestPatternBuilder.newRequestPattern(
                RequestMethod.POST,
                UrlPattern.fromOneOf(null, null, null, "/success", null)
            ))
            WIREMOCK_CALLBACK_SERVICE.resetRequests()
        }

        fun verifyFailureCallbackCall() {
            WIREMOCK_CALLBACK_SERVICE.verify(1, RequestPatternBuilder.newRequestPattern(
                RequestMethod.POST,
                UrlPattern.fromOneOf(null, null, null, "/failure", null)
            ))
            WIREMOCK_CALLBACK_SERVICE.resetRequests()
        }
    }

    @ConfigProperty(name = "quarkus.http.port")
    var assignedPort: Int? = 0

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00014")
    fun testTaskCreation() {
        val successUrl = "http://localhost:8081/success"
        val failureUrl = "http://localhost:8081/failure"
        startTrainEnrollmentTask(successUrl, failureUrl)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00014")
    fun testTaskCancellation() {
        val successUrl = "http://localhost:8081/success"
        val failureUrl = "http://localhost:8081/failure"

        val trainEnrollmentTask = startTrainEnrollmentTask(successUrl, failureUrl)
        val cancelUrl = trainEnrollmentTask.jsonPath().getString("cancel")

        cancelTrainEnrollmentTask(cancelUrl)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00014")
    fun testSuccessfulTrainEnrollment() {
        val trainSuccessStub = { WireMockTrain.stubAcceptAll() }
        val callbackSuccessVerify = { verifySuccessfulCallbackCall() }
        testTrainEnrollment(trainSuccessStub, callbackSuccessVerify, WireMockTrain.FRAMEWORK_NAME, null)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00014")
    fun testFailureTrainEnrollment() {
        val trainBadRequestStub = { WireMockTrain.stubForBadRequest() }
        val callbackFailureVerify = { verifyFailureCallbackCall() }
        testTrainEnrollment(trainBadRequestStub, callbackFailureVerify, WireMockTrain.FRAMEWORK_NAME, null)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00014")
    fun testSuccessfulTrainEnrollmentWithGivenEndpoint() {
        val trainSuccessStub = { WireMockTrain.stubAcceptAll() }
        val callbackSuccessVerify = { verifySuccessfulCallbackCall() }
        testTrainEnrollment(trainSuccessStub, callbackSuccessVerify, null, WireMockTrain.buildTrustListEndpoint())
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00014")
    fun testFailureTrainEnrollmentWithGivenEndpoint() {
        val trainBadRequestStub = { WireMockTrain.stubForBadRequest() }
        val callbackFailureVerify = { verifyFailureCallbackCall() }
        testTrainEnrollment(trainBadRequestStub, callbackFailureVerify, null, WireMockTrain.buildTrustListEndpoint())
    }

    private fun testTrainEnrollment(trainStub: () -> Unit, callbackVerify: () -> Unit, frameworkName: String?, trustListEndpoint: String?) {
        val successUrl = "${WIREMOCK_CALLBACK_SERVICE.baseUrl()}/success"
        val failureUrl = "${WIREMOCK_CALLBACK_SERVICE.baseUrl()}/failure"

        stubForCallbackService("/success")
        stubForCallbackService("/failure")
        trainStub.invoke()

        val trainEnrollmentTask = startTrainEnrollmentTask(successUrl, failureUrl)
        val redirectUrl = trainEnrollmentTask.jsonPath().getString("redirect")

        startTrainEnrollment(redirectUrl, frameworkName, trustListEndpoint)

        // Verify that Callback and Compliance service were called
        WireMockTrain.verifyTrainCall()
        callbackVerify.invoke()
    }

    private fun startTrainEnrollmentTask(successUrl: String, failureUrl: String) : ResponseBodyExtractionOptions {
        return RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .queryParam("profileId", UUID.randomUUID().toString())
            .queryParam("taskName", UUID.randomUUID().toString())
            .queryParam("success", successUrl)
            .queryParam("failure", failureUrl)
            .`when`()
            .post(TrainEnrollmentApi.BEGIN_ENROLLMENT_TASK)
            .then()
            .statusCode(200)
            .extract()
            .body()
    }

    private fun cancelTrainEnrollmentTask(cancelUrl: String) {
        val fixedUrl = fixUrl(cancelUrl)
        RestAssured.given()
            .`when`()
            .delete(fixedUrl)
            .then()
            .statusCode(204)
    }

    private fun startTrainEnrollment(redirectUrl: String, frameworkName: String?, trustListEndpoint: String?) {
        val fixedUrl = fixUrl(redirectUrl)
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            // taken from: https://gitlab.eclipse.org/eclipse/xfsc/train/tspa/-/blob/master/doc/operation/TSP-CRUD-Operations.md
            .body("""
                {
                    "frameworkName": ${if (frameworkName != null) "\"$frameworkName\"" else null},
                    "trustListEndpoint": ${if (trustListEndpoint != null) "\"$trustListEndpoint\"" else null},
                    "tspJson": {
                        "TrustServiceProvider": {
                            "UUID": "8271fcbf-0622-4415-b8b1-34ad74215dc6",
                            "TSPName": "CompanyaA Gmbh",
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
                                                "DID": "did:web:essif.iao.fraunhofer.de"
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
                }
            """.trimIndent())
            .`when`()
            .post(fixedUrl)
            .then()
            .statusCode(204)
    }

    @Throws(MalformedURLException::class)
    private fun fixUrl(url: String): String {
        return UriBuilder.fromUri(url).port(assignedPort!!).build().toURL().toString()
    }

}
