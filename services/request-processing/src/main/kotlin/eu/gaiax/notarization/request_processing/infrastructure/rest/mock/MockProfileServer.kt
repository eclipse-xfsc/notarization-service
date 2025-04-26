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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestListener
import com.github.tomakehurst.wiremock.http.Response
import eu.gaiax.notarization.api.profile.NoFilter
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.xfsc.not.api.serialization.EnhanceJWKSerializationCustomizer
import io.quarkus.arc.profile.UnlessBuildProfile
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
@UnlessBuildProfile("prod")
class MockProfileServer {
    @Inject
    lateinit var logger: Logger

    @ConfigProperty(name = "quarkus.rest-client.profile-api.url")
    lateinit var givenProfileServiceUrl: URL
    var wireMockServer: WireMockServer? = null
    fun startup(@Observes event: StartupEvent?) {
        logger.info("Preparing mock oauth2 server")
        val newWireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(givenProfileServiceUrl.port))
        val objectMapper = ObjectMapper()
        EnhanceJWKSerializationCustomizer().customize(objectMapper)
        objectMapper.registerModule(Jdk8Module())
        objectMapper.registerModule(JavaTimeModule())

        var singlePage = PagedView<Profile, NoFilter>()
        singlePage.pageCount = 1
        singlePage.size = 25
        singlePage.sort = SortDirection.Ascending
        singlePage.total = MockState.profiles.size.toLong()
        singlePage.items = MockState.profiles.toList()

        try {
            newWireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(profilesBasePath))
                    .withHeader("Accept", WireMock.matching(MediaType.APPLICATION_JSON))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withStatus(200)
                            .withBody(objectMapper.writeValueAsString(singlePage))
                    )
            )
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }

        try {
            newWireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(profileIdentifiersBasePath))
                    .withHeader("Accept", WireMock.matching(MediaType.APPLICATION_JSON))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withStatus(200)
                            .withBody(objectMapper.writeValueAsString(MockState.profiles.map { it.id }))
                    )
            )
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
        try {
            for (currentProfile in MockState.profiles) {
                newWireMockServer.stubFor(
                    WireMock.get(
                        WireMock.urlPathEqualTo(
                            "$profilesBasePath/" + URLEncoder.encode(
                                currentProfile.id,
                                StandardCharsets.UTF_8.toString()
                            )
                        )
                    )
                        .atPriority(1)
                        .withHeader("Accept", WireMock.matching(MediaType.APPLICATION_JSON))
                        .willReturn(
                            WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(200)
                                .withBody(objectMapper.writeValueAsString(currentProfile))
                        )
                )
            }
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
        newWireMockServer.stubFor(
            WireMock.get(WireMock.urlMatching("$profilesBasePath/.+"))
                .atPriority(10)
                .withHeader("Accept", WireMock.matching(MediaType.APPLICATION_JSON))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(404)
                )
        )
        newWireMockServer.addMockServiceRequestListener(RequestListener { inRequest: Request, inResponse: Response ->
            requestReceived(
                inRequest,
                inResponse
            )
        })
        newWireMockServer.start()
        wireMockServer = newWireMockServer
        logger.info("Prepared mock oauth2 server")
    }

    fun onStop(@Observes ev: ShutdownEvent?) {
        logger.info("Stopping mock oauth2 server")
        wireMockServer?.stop()
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debugv("Profile WireMock request at URL: {0}", inRequest.absoluteUrl)
        logger.debugv("Profile WireMock request headers: \n{0}", inRequest.headers)
        logger.debugv("Profile WireMock response body: \n{0}", inResponse.bodyAsString)
        logger.debugv("Profile WireMock response headers: \n{0}", inResponse.headers)
    }

    companion object {
        private const val profilesBasePath = "/api/v1/profiles"

        private const val profileIdentifiersBasePath = "/api/v1/profile-ids"
    }
}
