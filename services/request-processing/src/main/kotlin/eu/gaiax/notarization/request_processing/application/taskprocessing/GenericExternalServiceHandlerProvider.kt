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
package eu.gaiax.notarization.request_processing.application.taskprocessing

import eu.gaiax.notarization.api.profile.IssuanceAction
import eu.gaiax.notarization.api.profile.TaskDescription
import eu.gaiax.notarization.api.profile.WorkDescription
import eu.gaiax.notarization.request_processing.infrastructure.config.ExtensionServiceConfig
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import mu.KotlinLogging
import java.net.URI
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {}
/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class GenericExternalServiceHandlerProvider(
    private val extensionConfig: ExtensionServiceConfig,
): ExternalServiceHandlerProvider {
    private lateinit var extensionTasksByName: Map<String, ExtensionServiceConfig.ExternalServiceDescription>
    private lateinit var extensionActionsByName: Map<String, ExtensionServiceConfig.ExternalServiceDescription>
    private lateinit var taskServicesByName: Map<String, ExtensionServiceConfig.ExternalServiceDescription>
    private lateinit var actionServicesByName: Map<String, ExtensionServiceConfig.ExternalServiceDescription>

    fun onStartup(@Observes event: StartupEvent) {
        extensionTasksByName = organiseByName(extensionConfig.tasks())
        extensionActionsByName = organiseByName(extensionConfig.actions())
        taskServicesByName = extensionConfig.tasks().values.associateBy { it.serviceName() }
        actionServicesByName = extensionConfig.actions().values.associateBy { it.serviceName() }
    }

    override fun handleTask(task: TaskDescription): ExternalServiceExtensionHandler? {

        return determineServiceUri(task, taskServicesByName, extensionTasksByName)
    }
    override fun handleAction(action: IssuanceAction): ExternalServiceExtensionHandler? {

        return determineServiceUri(action, actionServicesByName, extensionActionsByName)
    }
    private fun determineServiceUri(
        task: WorkDescription,
        taskServicesByName: Map<String, ExtensionServiceConfig.ExternalServiceDescription>,
        extensionTasksByName: Map<String, ExtensionServiceConfig.ExternalServiceDescription>
    ): ExternalServiceExtensionHandler? {

        var targetLocation = task.serviceLocation
        var targetEncryptContent = task.encryptAtRest

        val serviceName = task.serviceName
        if (serviceName != null) {
            val foundService = taskServicesByName[serviceName]
            if (foundService != null) {
                targetLocation = targetLocation ?: foundService.location().getOrNull()
                targetEncryptContent = targetEncryptContent ?: foundService.encryptAtRest().getOrNull()
            }
        }
        val foundService = extensionTasksByName[task.name]

        if (foundService != null) {
            targetLocation = targetLocation ?: foundService.location().getOrNull()
            targetEncryptContent = targetEncryptContent ?: foundService.encryptAtRest().getOrNull()
        }
        if (targetLocation != null) {
            return FixedExternalServiceExtensionHandler(targetLocation, targetEncryptContent ?: true)
        }
        else {
            return null
        }
    }

    companion object {
        fun organiseByName(items: Map<String, ExtensionServiceConfig.ExternalServiceDescription>): Map<String, ExtensionServiceConfig.ExternalServiceDescription> {
            val results: MutableMap<String, ExtensionServiceConfig.ExternalServiceDescription> = mutableMapOf()
            for(entry in items) {
                val description = entry.value
                for (name in description.names().orElse(setOf())) {
                    if (results.containsKey(name)) {
                        logger.warn { "Ignoring the use of the service name $name by the extension service ${entry.key} because another service is already using it." }
                    } else {
                        results[name] = description
                    }
                }
            }
            return results
        }
    }
}

class FixedExternalServiceExtensionHandler(
    private val uri: URI,
    private val encryptAtRest: Boolean,
): ExternalServiceExtensionHandler {
    override fun createStartUri(): URI {
        return uri
    }

    override fun encryptAtRest(): Boolean {
        return encryptAtRest
    }

}
