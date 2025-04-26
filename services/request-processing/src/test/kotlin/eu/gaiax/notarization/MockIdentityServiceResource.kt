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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
/**
 *
 * @author Neil Crossley
 */
class MockIdentityServiceResource : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null
    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer!!.start()

        wireMockServer!!.addMockServiceRequestListener { `in`: Request, out: Response ->
            requestReceived(
                `in`,
                out
            )
        }

        // create some stubs
        return java.util.Map.of("browser-identification.url", wireMockServer!!.baseUrl() + "/session")
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug { " WireMock stub identity request at URL: ${inRequest.absoluteUrl}" }
        logger.debug { " WireMock stub identity request headers: ${inRequest.headers}" }
        logger.debug { " WireMock stub identity request body: \n${inRequest.bodyAsString}" }
        logger.debug { " WireMock stub identity response body: \n${inResponse.bodyAsString}" }
        logger.debug { " WireMock stub identity response headers: \n${inResponse.headers}" }
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
                IdentityWireMock::class.java, WireMockServer::class.java
            )
        )
    }
}
