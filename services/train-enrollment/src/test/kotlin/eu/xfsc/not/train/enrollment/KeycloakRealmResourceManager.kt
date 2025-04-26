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
 */

package eu.xfsc.not.train.enrollment

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.Options.ChunkedEncodingPolicy
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager


/**
 * @author Mike Prechtl
 */
class KeycloakRealmResourceManager : QuarkusTestResourceLifecycleManager {

    private lateinit var server: WireMockServer

    override fun start(): Map<String, String> {
        server = WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort().useChunkedTransferEncoding(ChunkedEncodingPolicy.NEVER)
        )

        server.start()

        server.stubFor(
            WireMock.post("/tokens")
                .withRequestBody(WireMock.matching("grant_type=password&username=alice&password=alice"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}"
                        )
                )
        )
        server.stubFor(
            WireMock.post("/tokens")
                .withRequestBody(WireMock.matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            "{\"access_token\":\"access_token_2\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}"
                        )
                )
        )

        val conf: MutableMap<String, String> = HashMap()
        conf["keycloak.url"] = server.baseUrl()
        return conf
    }

    @Synchronized
    override fun stop() {
        server.stop()
    }
}
