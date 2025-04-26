package eu.xfsc.not.ssi_issuance2.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class RevocationServiceMock : QuarkusTestResourceLifecycleManager {
    private var mockServer: WireMockServer = WireMockServer(
        WireMockConfiguration.options()
            .dynamicPort()
    )
    private val RESP = """{
       "id": "https://example.com/credentials/status/3#94567",
       "statusListCredential": "https://example.com/credentials/status/3",
       "statusListIndex": "94567",
       "statusPurpose": "revocation",
       "type": "StatusList2021Entry"
   }"""

    override fun start(): MutableMap<String, String> {
        mockServer.resetAll()
        mockServer.apply {
            start()
            stubFor(
                WireMock.post(
                    WireMock.urlMatching("/management/.*/entry")
                )
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(RESP)
                    )
            )
        }

        return mutableMapOf(("quarkus.rest-client.revocation_service.url" to mockServer.baseUrl()))
    }

    override fun stop() {
        mockServer.stop()
    }
}
