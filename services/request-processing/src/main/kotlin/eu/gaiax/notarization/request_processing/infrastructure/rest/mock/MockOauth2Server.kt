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
package eu.gaiax.notarization.request_processing.infrastructure.rest.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import io.quarkus.arc.profile.UnlessBuildProfile
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.net.URL

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
@UnlessBuildProfile("prod")
class MockOauth2Server {
    @Inject
    lateinit var logger: Logger

    @ConfigProperty(name = "quarkus.oidc.client-id")
    lateinit var givenClientId: String

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    lateinit var givenClientSecret: String

    @ConfigProperty(name = "quarkus.oidc.introspection-path")
    lateinit var givenIntrospectionUrl: URL

    var wireMockServer: WireMockServer? = null

    fun startup(@Observes event: StartupEvent?) {
        logger.info("Preparing mock oauth2 server")
        val newWireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(givenIntrospectionUrl.port))
        for (notary in MockState.notaries) {
            newWireMockServer.stubFor(
                WireMock.post(WireMock.urlEqualTo(givenIntrospectionUrl.path))
                    .atPriority(1)
                    .withBasicAuth(givenClientId, givenClientSecret)
                    .withRequestBody(WireMock.containing("token=" + notary.token))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBody(notary.introspectionToken())
                            .withStatus(200)
                    )
            )
        }
        newWireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo(givenIntrospectionUrl.path))
                .atPriority(10)
                .withBasicAuth(givenClientId, givenClientSecret)
                .withRequestBody(WireMock.matching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                            """
                                {"active":false}
                            """.trimIndent()
                        )
                        .withStatus(200)
                )
        )
        newWireMockServer.addMockServiceRequestListener { inRequest: Request, inResponse: Response ->
            requestReceived(
                inRequest,
                inResponse
            )
        }
        newWireMockServer.start()
        wireMockServer = newWireMockServer
        logger.info("Prepared mock oauth2 server")
    }

    fun onStop(@Observes ev: ShutdownEvent?) {
        logger.info("Stopping mock oauth2 server")
        wireMockServer?.stop()
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debugv("OAuth2 WireMock request at URL: {0}", inRequest.absoluteUrl)
        logger.debugv("OAuth2 WireMock request headers: \n{0}", inRequest.headers)
        logger.debugv("OAuth2 WireMock response body: \n{0}", inResponse.bodyAsString)
        logger.debugv("OAuth2 WireMock response headers: \n{0}", inResponse.headers)
    }
}
