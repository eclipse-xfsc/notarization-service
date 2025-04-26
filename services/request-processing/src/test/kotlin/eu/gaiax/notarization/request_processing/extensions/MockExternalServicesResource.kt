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
package eu.gaiax.notarization.request_processing.extensions

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import eu.gaiax.notarization.api.profile.IssuanceAction
import eu.gaiax.notarization.api.profile.WorkDescription
import eu.gaiax.notarization.request_processing.domain.model.WorkType
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
/**
 *
 * @author Neil Crossley
 */
class MockExternalServicesResource : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null

    override fun start(): Map<String, String> {
        val wiremock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer = wiremock
        wiremock.start()

        wiremock.addMockServiceRequestListener { `in`: Request, out: Response ->
            requestReceived(
                `in`,
                out
            )
        }

        val actionServiceByName = extractServiceDescriptions(MockState.issuanceActions(), WorkType.Action)
        val taskServiceByName = extractServiceDescriptions(MockState.taskDescriptions().filter { !it.name.contains("upload") }, WorkType.Task)

        val settings = mutableMapOf<String, String>()
        for (entry in actionServiceByName + taskServiceByName) {
            val description = entry.value

            val workType = when(description.workType) {
                WorkType.Task -> "tasks"
                WorkType.Action -> "actions"
                else -> continue
            }

            val extensionPath = "/external/${description.workType}/${description.serviceName}"
            settings.putAll(
                mapOf(
                    "gaia-x.extensions.${workType}.${entry.key}.location" to wireMockServer!!.baseUrl() + extensionPath,
                    "gaia-x.extensions.${workType}.${entry.key}.service-name" to description.serviceName,
                    "gaia-x.extensions.${workType}.${entry.key}.names" to description.names.joinToString(","),
                    "gaia-x.extensions.${workType}.${entry.key}.encrypt-at-rest" to "true",
                )
            )
            addExtensionService(wiremock, extensionPath, entry.value)
        }
        // create some stubs
        return settings
    }

    private fun extractServiceDescriptions(
        issuanceActions: List<WorkDescription>,
        workType: WorkType
    ): MutableMap<String, ExternalServiceDescription> {
        val serviceByName = mutableMapOf<String, ExternalServiceDescription>()
        for (action in issuanceActions) {
            val serviceName = action.serviceName
            if (serviceName == null) {
                val actionName = action.name
                val foundService = serviceByName[actionName]
                if (foundService == null) {
                    serviceByName[actionName] = ExternalServiceDescription(actionName, workType, actionName)
                }
            } else {
                val foundService = serviceByName[serviceName]
                if (foundService == null) {
                    serviceByName[serviceName] = ExternalServiceDescription(serviceName, workType, action.name)
                } else {
                    foundService.names.add(action.name)
                }
            }
        }
        return serviceByName
    }

    fun addExtensionService(wiremock: WireMockServer, path: String, service: ExternalServiceDescription) {

        val cancelPath = "/cancel/${service.workType}/${service.serviceName}"
        val redirectFromIdentityService =
            """
                {
                    "redirect": "http://${service.serviceName}.${service.workType}/user/NONCE",
                    "cancel": "${wiremock.baseUrl()}${cancelPath}"
                }
            """.trimIndent()

        wiremock.stubFor(
            WireMock.post(WireMock.urlMatching("$path.+success.+failure.+"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(redirectFromIdentityService)
                        .withStatus(200)
                )
        )
        wiremock.stubFor(
            WireMock.delete(WireMock.urlMatching("$cancelPath.+"))
                .willReturn(
                    WireMock.noContent()
                )
        )
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug { " WireMock stub train enrolment request at URL: ${inRequest.absoluteUrl}" }
        logger.debug { " WireMock stub train enrolment request headers: ${inRequest.headers}" }
        logger.debug { " WireMock stub train enrolment request body: \n${inRequest.bodyAsString}" }
        logger.debug { " WireMock stub train enrolment response body: \n${inResponse.bodyAsString}" }
        logger.debug { " WireMock stub train enrolment response headers: \n${inResponse.headers}" }
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
                ExternalServicesWireMock::class.java, WireMockServer::class.java
            )
        )
    }
    companion object {
        class ExternalServiceDescription(val serviceName: String, val workType: WorkType, val names: MutableSet<String>) {
            constructor(serviceName: String, workType: WorkType, name: String): this(serviceName, workType, mutableSetOf(name))
        }

    }
}
