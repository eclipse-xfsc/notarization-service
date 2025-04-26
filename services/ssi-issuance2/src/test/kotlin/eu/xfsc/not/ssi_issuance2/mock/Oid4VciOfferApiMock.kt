package eu.xfsc.not.ssi_issuance2.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class Oid4VciOfferApiMock : QuarkusTestResourceLifecycleManager {

    private var mockServer: WireMockServer = WireMockServer(
        WireMockConfiguration.options()
            .dynamicPort()
    )

    override fun start(): MutableMap<String, String> {
        mockServer.resetAll()
        mockServer.apply {
            start()
            stubFor(
                WireMock.post(
                    WireMock.urlMatching("/api/v1/oid4vci/offer/credential-offer.*")
                )
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("http://offer.url")
                    )
            )

        }
        return mutableMapOf(("quarkus.rest-client.offer_api.url" to mockServer.baseUrl()))
    }

    override fun stop() {
        mockServer.stop()
    }
}

