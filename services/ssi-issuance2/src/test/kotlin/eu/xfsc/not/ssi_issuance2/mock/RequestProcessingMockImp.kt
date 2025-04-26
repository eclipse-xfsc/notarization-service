package eu.xfsc.not.ssi_issuance2.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import mu.KotlinLogging

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RequestProcessingMock

private val logger = KotlinLogging.logger {}

class RequestProcessingMockImp : QuarkusTestResourceLifecycleManager {
    private var wireMockServer: WireMockServer? = null
    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()).also { wm ->
            wm.start()

            wm.stubFor(
                WireMock.post(
                    WireMock.urlMatching("/.*")
                ).willReturn(WireMock.aResponse().withStatus(200))
            )

            wm.addMockServiceRequestListener { `in`: Request, out: Response ->
                requestReceived(
                    `in`,
                    out
                )
            }
        }

        return java.util.Map.of()
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug { " WireMock stub requestproc request at URL: ${inRequest.absoluteUrl}" }
        logger.debug { " WireMock stub requestproc request headers: ${inRequest.headers}" }
        logger.debug { " WireMock stub requestproc request body: \n${inRequest.bodyAsString}" }
        logger.debug { " WireMock stub requestproc response body: \n${inResponse.bodyAsString}" }
        logger.debug { " WireMock stub requestproc response headers: \n${inResponse.headers}" }
    }

    @Synchronized
    override fun stop() {
        wireMockServer = wireMockServer?.let {
            it.stop()
            null
        }
    }

    override fun inject(testInjector: QuarkusTestResourceLifecycleManager.TestInjector) {
        testInjector.injectIntoFields(
            wireMockServer,
            QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType(
                RequestProcessingMock::class.java, WireMockServer::class.java
            )
        )
    }
}
