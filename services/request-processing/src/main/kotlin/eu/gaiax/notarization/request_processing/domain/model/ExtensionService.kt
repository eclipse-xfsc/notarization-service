package eu.gaiax.notarization.request_processing.domain.model

import java.net.URI

open class ServiceDescription (
    val serviceName: String,
    val names: Set<String>,
    val serviceLocation: URI?,
    val description: String?,
    val encryptContent: Boolean?
    )

class PartialActionServiceDescription(
    serviceName: String,
    names: Set<String>,
    serviceLocation: URI?,
    description: String?,
    encryptContent: Boolean?
) : ServiceDescription(serviceName, names, serviceLocation, description, encryptContent) {
}

class PartialTaskServiceDescription(
    serviceName: String,
    names: Set<String>,
    serviceLocation: URI?,
    description: String?,
    encryptContent: Boolean?
) : ServiceDescription(serviceName, names, serviceLocation, description, encryptContent) {
}
