package eu.gaiax.notarization.request_processing.application.taskprocessing

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.profile.IssuanceAction
import eu.gaiax.notarization.api.profile.TaskDescription
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import io.smallrye.mutiny.Uni
import java.net.URI

interface ExternalServiceHandlerProvider {

    fun handleTask(task: TaskDescription): ExternalServiceExtensionHandler?
    fun handleAction(action: IssuanceAction): ExternalServiceExtensionHandler?
}

interface ExternalServiceExtensionHandler {
    fun createStartUri(): URI
    fun encryptAtRest(): Boolean
}

interface InternalServiceHandlerProvider {
    fun handleTask(task: TaskDescription): InternalServiceExtensionHandler?
}

interface InternalServiceExtensionHandler {

    fun createStartUri(id: TaskId): Uni<URI?>

    fun cancelWork(id: TaskId): Uni<Void>
    fun finishWorkSuccess(sessionTask: SessionTask, data: JsonNode?): Uni<Void>
    fun finishWorkFail(sessionTask: SessionTask, data: JsonNode?): Uni<Void>
}
