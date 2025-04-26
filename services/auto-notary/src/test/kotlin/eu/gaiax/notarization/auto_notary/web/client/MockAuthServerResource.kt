package eu.gaiax.notarization.auto_notary.web.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.Options.ChunkedEncodingPolicy
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType
import java.util.*

/**
 *
 * @author Neil Crossley
 */
class MockAuthServerResource : QuarkusTestResourceLifecycleManager {
    private var wireMockServer: WireMockServer? = null
    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(
            WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .useChunkedTransferEncoding(ChunkedEncodingPolicy.NEVER)
        )
        wireMockServer!!.start()
        wireMockServer!!.stubFor(
            WireMock.post("/tokens")
                .withRequestBody(
                    WireMock.matching(
                        String.format(
                            "grant_type=password&username=%s&password=%s&client_id=%s",
                            username,
                            password,
                            clientId
                        )
                    )
                )
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            "{\"access_token\":\"" + ACCESS_TOKEN_1 + "\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}"
                        )
                )
        )
        wireMockServer!!.stubFor(
            WireMock.post("/tokens")
                .withRequestBody(WireMock.matching("grant_type=refresh_token&refresh_token=" + ACCESS_TOKEN_1))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            "{\"access_token\":\"access_token_2\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}"
                        )
                )
        )
        return java.util.Map.of(
            "quarkus.oidc-client.auth-server-url", wireMockServer!!.baseUrl(),
            "quarkus.oidc-client.grant-options.password.username", username,
            "quarkus.oidc-client.grant-options.password.password", password,
            "quarkus.oidc-client.client-id", clientId
        )
    }

    @Synchronized
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
                MockAuthServer::class.java, WireMockServer::class.java
            )
        )
    }

    companion object {
        val ACCESS_TOKEN_1 = UUID.randomUUID().toString()
        val username = "user" + UUID.randomUUID().toString()
        val password = "password" + UUID.randomUUID().toString()
        val clientId = "clientId" + UUID.randomUUID().toString()
    }
}
