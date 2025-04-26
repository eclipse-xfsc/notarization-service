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
package eu.gaiax.notarization.profile.infrastructure.rest.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType
import io.quarkus.test.junit.QuarkusTestProfile


private const val clientV1 = "quarkus.rest-client.ssi-issuance-v1-api.url"

private const val clientV2 = "quarkus.rest-client.ssi-issuance-v2-api.url"

/**
 *
 * @author Neil Crossley
 */
class MockSsiIssuanceV1Resource : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null
    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer!!.start()
        return java.util.Map.of(clientV1, wireMockServer!!.baseUrl())
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
                SsiIssuanceV1WireMock::class.java, WireMockServer::class.java
            )
        )
    }
}


class MockSsiIssuanceV2Resource : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null
    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer!!.start()
        return java.util.Map.of(clientV2, wireMockServer!!.baseUrl())
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
                SsiIssuanceV2WireMock::class.java, WireMockServer::class.java
            )
        )
    }
}

class NoSsiIssuanceV1Profile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {

        return mapOf(
            clientV1 to "",
            clientV2 to "http://ssi-v1:8080"
        )
    }
}

class NoSsiIssuanceV2Profile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {

        return mapOf(
            clientV1 to "http://ssi-v1:8080",
            clientV2 to ""
        )
    }
}

class NoSsiIssuanceProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {

        return mapOf(
            clientV1 to "",
            clientV2 to ""
        )
    }
}
