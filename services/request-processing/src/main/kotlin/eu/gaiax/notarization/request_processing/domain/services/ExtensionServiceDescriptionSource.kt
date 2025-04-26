package eu.gaiax.notarization.request_processing.domain.services

import eu.gaiax.notarization.request_processing.domain.model.PartialActionServiceDescription
import eu.gaiax.notarization.request_processing.domain.model.PartialTaskServiceDescription

interface ExtensionServiceDescriptionSource {

    fun taskDescription(taskName: String): PartialTaskServiceDescription?

    fun actionDescription(actionName: String): PartialActionServiceDescription?
}
