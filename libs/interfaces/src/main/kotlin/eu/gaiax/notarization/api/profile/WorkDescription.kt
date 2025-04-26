package eu.gaiax.notarization.api.profile

import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI

open class WorkDescription(
    @get:Schema(description = "The identifier of the task or action. It must be unique within a given profile.")
    open val name: String,
    @get:Schema(description = "A human readable description of the task or action.")
    open val description: String?,
    @get:Schema(description =
    """The identifier of the extension service that executes this task or action.

This is optional when the request-processing service has a configured association for the task/action name with a known extension service.""")
    open val serviceName: String?,
    open val serviceLocation: URI?,
    open val encryptAtRest: Boolean?
) {
}
