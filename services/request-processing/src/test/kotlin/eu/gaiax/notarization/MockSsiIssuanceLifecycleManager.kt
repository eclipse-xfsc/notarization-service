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
package eu.gaiax.notarization

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import eu.gaiax.notarization.api.issuance.IssuanceInitResponse
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.SsiIssuanceRestClient
import eu.xfsc.not.api.serialization.EnhanceJWKSerializationCustomizer
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
class MockSsiIssuanceLifecycleManager : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null
    var wireMockServerV2: WireMockServer? = null

    private val resp: SsiIssuanceRestClient.IssuanceResponse
        private get() {
            val resp = SsiIssuanceRestClient.IssuanceResponse()
            resp.invitationURL = URI.create("")
            return resp
        }

    private val respV2: IssuanceInitResponse
        private get() {
            val resp = IssuanceInitResponse(
                URI.create(""),
                URI.create(""),
            )
            return resp
        }

    override fun start(): Map<String, String> {
        return try {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServerV2 = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer!!.start()
            wireMockServerV2!!.start()
            val objectMapper = jacksonObjectMapper()
            EnhanceJWKSerializationCustomizer().customize(objectMapper)
            objectMapper.registerModule(Jdk8Module())
            objectMapper.registerModule(JavaTimeModule())
            wireMockServer!!.stubFor(
                WireMock.post(WireMock.urlMatching("/credential/start-issuance/"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withStatus(200)
                            .withBody(objectMapper.writeValueAsString(this.resp))
                    )
            )
            wireMockServerV2!!.stubFor(
                WireMock.post(WireMock.urlMatching("/api/v2/issuance/session"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withStatus(200)
                            .withBody(objectMapper.writeValueAsString(this.respV2))
                    )
                )
            wireMockServer!!.addMockServiceRequestListener { `in`: Request, out: Response ->
                requestReceived(
                    `in`,
                    out
                )
            }
            wireMockServerV2!!.addMockServiceRequestListener { `in`: Request, out: Response ->
                requestReceived(
                    `in`,
                    out
                )
            }
            logger.debug { "Wiremock available at: ${wireMockServerV2!!.baseUrl()}" }
            // create some stubs
            mapOf(
                "quarkus.rest-client.ssi-issuance-v1-api.url" to wireMockServer!!.baseUrl(),
                "quarkus.rest-client.ssi-issuance-v2-api.url" to wireMockServerV2!!.baseUrl(),
            )
        } catch (ex: JsonProcessingException) {
            throw RuntimeException(ex)
        }
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug { " WireMock stub SSI issuance request at URL: ${inRequest.absoluteUrl}" }
        logger.debug { " WireMock stub SSI issuance request headers: \n${inRequest.headers}" }
        logger.debug { " WireMock stub SSI issuance request body: \n${inRequest.bodyAsString}" }
        logger.debug { " WireMock stub SSI issuance response body: \n${inResponse.bodyAsString}" }
        logger.debug { " WireMock stub SSI issuance response headers: \n${inResponse.headers}" }
    }

    override fun stop() {
        if (wireMockServer != null) {
            wireMockServer!!.stop()
            wireMockServer = null
        }
    }

    override fun inject(testInjector: TestInjector) {
        testInjector.injectIntoFields(
            wireMockServer,
            AnnotatedAndMatchesType(
                SsiIssuanceWireMock::class.java, WireMockServer::class.java
            )
        )
        testInjector.injectIntoFields(
            wireMockServerV2,
            AnnotatedAndMatchesType(
                SsiIssuanceV2WireMock::class.java, WireMockServer::class.java
            )
        )
    }
}
