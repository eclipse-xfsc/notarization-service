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

package eu.xfsc.not.compliance.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.UrlPattern
import eu.xfsc.not.compliance.WireMockCompliance
import eu.xfsc.not.compliance.WireMockCompliance.Companion.verifyComplianceCall
import eu.xfsc.not.compliance.rest.resources.ComplianceTaskApi
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


private val logger = KotlinLogging.logger {}


/**
 * @author Mike Prechtl
 */
@QuarkusTest
@QuarkusTestResource(WireMockCompliance::class)
class ComplianceRestTest {

    companion object {
        val WIREMOCK_CALLBACK_SERVICE: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

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
                WireMock
                    .post(WireMock.urlMatching(urlPattern))
                    .willReturn(WireMock.noContent())
            )
        }

        protected fun requestReceived(inRequest: Request, inResponse: Response) {
            logger.info { "WireMock callback stub request at URL: ${inRequest.absoluteUrl}" }
            logger.info { "WireMock callback stub request headers: \n${inRequest.headers}" }
            logger.info { "WireMock callback stub request body: \n${inRequest.bodyAsString}" }
        }

        fun verifySuccessfulCallbackCall() {
            WIREMOCK_CALLBACK_SERVICE.verify(1, RequestPatternBuilder.newRequestPattern(RequestMethod.POST,
                UrlPattern.fromOneOf(null, null, null, "/success", null)))
        }

        fun verifyFailureCallbackCall() {
            WIREMOCK_CALLBACK_SERVICE.verify(1, RequestPatternBuilder.newRequestPattern(RequestMethod.POST,
                UrlPattern.fromOneOf(null, null, null, "/failure", null)))
        }
    }

    @ConfigProperty(name = "quarkus.http.port")
    var assignedPort: Int? = 0

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun testTaskCreation() {
        val successUrl = "http://localhost:8081/success"
        val failureUrl = "http://localhost:8081/failure"
        startComplianceTask(successUrl, failureUrl)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun testTaskCancellation() {
        val successUrl = "http://localhost:8081/success"
        val failureUrl = "http://localhost:8081/failure"

        val complianceStartRespBody = startComplianceTask(successUrl, failureUrl)
        val cancelUrl = complianceStartRespBody.jsonPath().getString("cancel")

        cancelComplianceTask(cancelUrl)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun testSuccessfulComplianceCheck() {
        val complianceSuccessStub = { WireMockCompliance.stubAcceptAll() }
        val callbackSuccessVerify = { verifySuccessfulCallbackCall() }
        testComplianceCheck(complianceSuccessStub, callbackSuccessVerify)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun testFailureComplianceCheck() {
        val complianceBadRequestStub = { WireMockCompliance.stubForBadRequest() }
        val callbackFailureVerify = { verifyFailureCallbackCall() }
        testComplianceCheck(complianceBadRequestStub, callbackFailureVerify)
    }

    private fun testComplianceCheck(complianceStub: () -> Unit, callbackVerify: () -> Unit) {
        val successUrl = "${WIREMOCK_CALLBACK_SERVICE.baseUrl()}/success"
        val failureUrl = "${WIREMOCK_CALLBACK_SERVICE.baseUrl()}/failure"

        stubForCallbackService("/success")
        stubForCallbackService("/failure")
        complianceStub.invoke()

        val complianceStartRespBody = startComplianceTask(successUrl, failureUrl)
        val redirectUrl = complianceStartRespBody.jsonPath().getString("redirect")

        submitVerifiablePresentation(redirectUrl)

        // Verify that Callback and Compliance service were called
        verifyComplianceCall()
        callbackVerify.invoke()
    }

    private fun startComplianceTask(successUrl: String, failureUrl: String) : ResponseBodyExtractionOptions {
        return RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .queryParam("success", successUrl)
            .queryParam("failure", failureUrl)
            .`when`()
            .post(ComplianceTaskApi.BEGIN_COMPLIANCE_TASK)
            .then()
            .statusCode(200)
            .extract()
            .body()
    }

    private fun cancelComplianceTask(cancelUrl: String) {
        val fixedUrl = fixUrl(cancelUrl)
        RestAssured.given()
            .`when`()
            .delete(fixedUrl)
            .then()
            .statusCode(204)
    }

    private fun submitVerifiablePresentation(redirectUrl: String) {
        val fixedUrl = fixUrl(redirectUrl)
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "@context": "https://www.w3.org/2018/credentials/v1",
                  "type": "VerifiablePresentation",
                  "verifiableCredential": [
                    {
                      "@context": [
                        "https://www.w3.org/2018/credentials/v1",
                        "https://w3id.org/security/suites/jws-2020/v1"
                      ],
                      "type": [
                        "VerifiableCredential"
                      ]
                    }
                  ],
                  "_comment" : "more to come here"
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
