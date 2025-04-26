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

package eu.xfsc.not.compliance

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
class WireMockCompliance : QuarkusTestResourceLifecycleManager {

    companion object {
        val WIREMOCK_SERVER: WireMockServer = WireMockServer(wireMockConfig().dynamicPort())

        @JvmStatic
        fun verifyComplianceCall() {
            WIREMOCK_SERVER.verify(1, RequestPatternBuilder.newRequestPattern(
                RequestMethod.POST,
                UrlPattern.fromOneOf(null, null, null, "/api/credential-offers", null)))
            WIREMOCK_SERVER.resetRequests()
        }

        @JvmStatic
        fun stubAcceptAll() {
            WIREMOCK_SERVER.stubFor(
                WireMock.post(WireMock.urlMatching("/api/credential-offers"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBody("""
                                {
                                  "@context": [
                                    "https://www.w3.org/2018/credentials/v1",
                                    "https://w3id.org/security/suites/jws-2020/v1",
                                    "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
                                  ],
                                  "type": [
                                    "VerifiableCredential"
                                  ],
                                  "id": "https://storage.gaia-x.eu/credential-offers/b3e0a068-4bf8-4796-932e-2fa83043e203",
                                  "issuer": "did:web:compliance.lab.gaia-x.eu:main",
                                  "issuanceDate": "2024-02-16T13:17:51.301Z",
                                  "expirationDate": "2024-05-16T13:17:51.301Z",
                                  "credentialSubject": [
                                    {
                                      "type": "gx:compliance",
                                      "id": "https://gaia-x.eu/legalRegistrationNumberVC.json",
                                      "gx:integrity": "sha256-57d596833c0f3538f7690638a7e0b84db5cdf1bd0f2d30a5b75d7c632853cad1",
                                      "gx:integrityNormalization": "RFC8785:JCS",
                                      "gx:version": "22.10",
                                      "gx:type": "gx:legalRegistrationNumber"
                                    }
                                  ],
                                  "_comment": "more to come here"
                                }
                            """.trimIndent())
                            .withStatus(201)
                    )
            )
        }

        @JvmStatic
        fun stubForBadRequest() {
            WIREMOCK_SERVER.stubFor(
                WireMock.post(WireMock.urlMatching("/api/credential-offers"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBody("""
                                {
                                  "statusCode": 409,
                                  "message": {
                                    "conforms": false,
                                    "results": [
                                      "Just an error. :)"
                                    ]
                                  },
                                  "error": "Conflict"
                                }
                            """.trimIndent())
                            .withStatus(409)
                    )
            )
        }
    }

    override fun start(): Map<String, String> {
        WIREMOCK_SERVER.start()
        return Collections.singletonMap("quarkus.rest-client.compliance.url", WIREMOCK_SERVER.baseUrl())
    }

    override fun stop() {
        WIREMOCK_SERVER.stop()
    }
}
