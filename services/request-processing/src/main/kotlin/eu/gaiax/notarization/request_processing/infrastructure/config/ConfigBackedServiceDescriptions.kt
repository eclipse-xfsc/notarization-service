package eu.gaiax.notarization.request_processing.infrastructure.config

import eu.gaiax.notarization.request_processing.domain.model.PartialActionServiceDescription
import eu.gaiax.notarization.request_processing.domain.model.PartialTaskServiceDescription
import eu.gaiax.notarization.request_processing.domain.services.ExtensionServiceDescriptionSource
import io.quarkus.runtime.StartupEvent
import io.smallrye.config.Priorities
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlin.jvm.optionals.getOrNull

@ApplicationScoped
class ConfigBackedServiceDescriptions(
    val config: ExtensionServiceConfig
): ExtensionServiceDescriptionSource {

    lateinit var tasksByName: Map<String, PartialTaskServiceDescription>
    lateinit var actionsByName: Map<String, PartialActionServiceDescription>

    fun onStartup(@Observes @Priority(Priorities.PLATFORM) event: StartupEvent) {
        tasksByName = config.tasks().mapValues { asTask(it.value) }
        actionsByName = config.actions().mapValues { asAction(it.value) }
    }

    override fun taskDescription(taskName: String): PartialTaskServiceDescription? {
        return tasksByName[taskName]
    }

    override fun actionDescription(actionName: String): PartialActionServiceDescription? {
        return actionsByName[actionName]
    }

    private fun asTask(item: ExtensionServiceConfig.ExternalServiceDescription): PartialTaskServiceDescription {
        return PartialTaskServiceDescription(
            item.serviceName(),
            item.names().orElse(setOf()),
            item.location().getOrNull(),
            item.description().getOrNull(),
            item.encryptAtRest().getOrNull()
        )
    }
    private fun asAction(item: ExtensionServiceConfig.ExternalServiceDescription): PartialActionServiceDescription {
        return PartialActionServiceDescription(
            item.serviceName(),
            item.names().orElse(setOf()),
            item.location().getOrNull(),
            item.description().getOrNull(),
            item.encryptAtRest().getOrNull()
        )
    }
}
