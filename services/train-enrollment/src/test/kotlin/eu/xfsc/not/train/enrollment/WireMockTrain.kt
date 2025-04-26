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

package eu.xfsc.not.train.enrollment

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.UrlPattern
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import jakarta.ws.rs.core.MediaType
import java.util.Collections


/**
 * @author Mike Prechtl
 */
class WireMockTrain : QuarkusTestResourceLifecycleManager {

    companion object {
        val WIREMOCK_SERVER: WireMockServer = WireMockServer(wireMockConfig().dynamicPort())

        const val FRAMEWORK_NAME: String = "framework-name"

        @JvmStatic
        fun verifyTrainCall() {
            WIREMOCK_SERVER.verify(1, RequestPatternBuilder.newRequestPattern(
                RequestMethod.PUT,
                UrlPattern.fromOneOf(null, null, null, "/tspa/v1/${FRAMEWORK_NAME}/trust-list/tsp", null)))
            WIREMOCK_SERVER.resetRequests()
        }

        @JvmStatic
        fun stubAcceptAll() {
            WIREMOCK_SERVER.stubFor(
                WireMock.put(WireMock.urlMatching("/tspa/v1/${FRAMEWORK_NAME}/trust-list/tsp"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withStatus(201)
                    )
            )
        }

        @JvmStatic
        fun stubForBadRequest() {
            WIREMOCK_SERVER.stubFor(
                WireMock.put(WireMock.urlMatching("/tspa/v1/${FRAMEWORK_NAME}/trust-list/tsp"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBody("""
                                [
                                    "status": "400",
                                    "message": "Bad Request"
                                ]
                            """.trimIndent())
                            .withStatus(400)
                    )
            )
        }

        @JvmStatic
        fun buildTrustListEndpoint(): String {
            return "${WIREMOCK_SERVER.baseUrl()}/tspa/v1/${FRAMEWORK_NAME}/trust-list/tsp"
        }
    }

    override fun start(): Map<String, String> {
        WIREMOCK_SERVER.start()
        return Collections.singletonMap("quarkus.rest-client.train.url", WIREMOCK_SERVER.baseUrl())
    }

    override fun stop() {
        WIREMOCK_SERVER.stop()
    }
}
