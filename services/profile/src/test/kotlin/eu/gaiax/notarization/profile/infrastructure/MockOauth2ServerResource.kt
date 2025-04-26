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
package eu.gaiax.notarization.profile.infrastructure

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import eu.gaiax.notarization.profile.infrastructure.rest.client.SsiIssuanceV2WireMock
import io.quarkus.arc.profile.UnlessBuildProfile
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import java.time.Instant
import java.util.HashSet
import java.util.UUID
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class InjectOauth2Controls

class Notary(
    val subject: String,
    val name: String,
    val roles: Set<String>,
    val token: String
) {
    fun bearerValue(): String {
        return String.format("Bearer %s", this.token)
    }

    fun introspectionToken(): String {
        val rolesString = roles.map { "\"${it}\"" }.joinToString(",")
        val spacedRoles = roles.joinToString(" ")
        return String.format(
            """
                    {
                      "sub": "$subject",
                      "iss": "mocked-idp",
                      "username": "$name",
                      "roles": [
                        "default-roles-notarization-realm",
                        "offline_access",
                        "uma_authorization",
                        $rolesString
                      ],
                      "exp": ${Instant.now().plusSeconds(300).epochSecond},
                      "typ": "Bearer",
                      "scope": "email profile $spacedRoles",
                      "client_id": "portal-client",
                      "token_type": "bearer",
                      "active": true
                    }
                   """
        )
    }
}

class Outh2ServerControls(private val wireMockServer: WireMockServer) {

    val stubs = mutableListOf<StubMapping>()

    fun addNotary(notary: Notary) {
        val stub = wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo(MockOauth2ServerResource.introspectionPath))
                .atPriority(1)
                .withBasicAuth(MockOauth2ServerResource.clientId, MockOauth2ServerResource.clientSecret)
                .withRequestBody(WireMock.containing("token=" + notary.token))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(notary.introspectionToken())
                        .withStatus(200)
                )
        )
    }
    fun reset() {
        val currentStubs = stubs.toList()
        stubs.clear()
        for (stub in currentStubs) {
            wireMockServer.removeStub(stub)
        }
    }
}

class MockOauth2ServerResource : QuarkusTestResourceLifecycleManager {

    companion object {
        val introspectionPath = "/introspection-path"
        val realmPath = "/realm-path"
        val clientId = UUID.randomUUID().toString()
        val clientSecret = UUID.randomUUID().toString()
    }

    var wireMockServer: WireMockServer? = null
    var controls: Outh2ServerControls? = null

    override fun start(): MutableMap<String, String> {
        logger.info("Preparing mock oauth2 server")

        val newWireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

        newWireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo(introspectionPath))
                .atPriority(10)
                .withBasicAuth(clientId, clientSecret)
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

        newWireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("$realmPath/.well-known/openid-configuration"))
                //.withHeader("Authorization", WireMock.matching(".*"))
                //.withRequestBody(WireMock.matching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withBody(
                            """
                                {
                                  "issuer": "${newWireMockServer.url("issuer")}",
                                  "token_endpoint": "${newWireMockServer.url(introspectionPath)}",
                                  "userinfo_endpoint": "${newWireMockServer.url("introspectionPath")}",
                                  "scopes_supported": [
                                    "notary"
                                  ],
                                  "response_types_supported": [
                                    "code",
                                    "id_token",
                                    "token id_token"
                                  ],
                                  "token_endpoint_auth_methods_supported": [
                                    "client_secret_basic"
                                  ]
                                }
                            """.trimIndent()
                        )
                )
        )

        wireMockServer = newWireMockServer
        controls = Outh2ServerControls(newWireMockServer)
        logger.info { "Prepared mock oauth2 server at: ${newWireMockServer.baseUrl()}" }

        return mutableMapOf(
            "quarkus.oidc.introspection-path" to newWireMockServer.url(introspectionPath),
            "quarkus.oidc.client-id" to clientId,
            "quarkus.oidc.credentials.secret" to clientSecret,
            "quarkus.oidc.roles.role-claim-path" to "scope",
            "quarkus.oidc.auth-server-url" to newWireMockServer.url(realmPath),
        )
    }

    fun onStop(@Observes ev: ShutdownEvent?) {
        logger.info("Stopping mock oauth2 server")
    }

    override fun stop() {
        val currentMock = wireMockServer
        if (currentMock != null) {
            currentMock.stop()
            wireMockServer = null
        }
    }

    override fun inject(testInjector: QuarkusTestResourceLifecycleManager.TestInjector) {
        testInjector.injectIntoFields(
            controls!!,
            QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType(
                InjectOauth2Controls::class.java, Outh2ServerControls::class.java
            )
        )
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug { "OAuth2 WireMock request at URL: ${inRequest.absoluteUrl}"}
        logger.debug { "OAuth2 WireMock request headers: ${inRequest.headers}" }
        logger.debug { "OAuth2 WireMock response body: ${inResponse.bodyAsString}" }
        logger.debug { "OAuth2 WireMock response headers: ${inResponse.headers}" }
    }

}
