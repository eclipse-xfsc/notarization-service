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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import java.io.IOException
import java.net.ServerSocket

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
class MockServicesLifecycleManager : QuarkusTestResourceLifecycleManager {
    var mockProfilePort = 0
    var mockOAuth2Port = 0
    override fun start(): Map<String, String> {
        mockProfilePort = findFreePort()
        mockOAuth2Port = findFreePort()
        val introspectionUrl =
            UriBuilder.fromUri("http://localhost:9194/realms/notarization-realm/protocol/openid-connect/token/introspect")
                .port(mockOAuth2Port).build()
        val profileUrl = UriBuilder.fromUri("http://localhost")
            .port(mockProfilePort).build()
        logger.info { "Assigning mock ports $mockProfilePort $mockOAuth2Port" }
        return java.util.Map.of(
            "quarkus.oidc.introspection-path", introspectionUrl.toString(),
            "quarkus.rest-client.profile-api.url", profileUrl.toString()
        )
    }

    override fun stop() {
        releasePort(mockProfilePort)
        releasePort(mockOAuth2Port)
    }

    companion object {
        private fun findFreePort(): Int {
            var port = 0
            // For ServerSocket port number 0 means that the port number is automatically allocated.
            try {
                ServerSocket(0).use { socket ->
                    // Disable timeout and reuse address after closing the socket.
                    socket.reuseAddress = true
                    port = socket.localPort
                }
            } catch (ignored: IOException) {
            }
            if (port > 0) {
                return port
            }
            throw RuntimeException("Could not find a free port")
        }

        private fun releasePort(port: Int) {
            if (port <= 0) {
                return
            }
            try {
                ServerSocket(port).use { socket ->
                    // No longer enabling socket reuse.
                    socket.reuseAddress = false
                }
            } catch (ignored: IOException) {
            }
        }
    }
}
