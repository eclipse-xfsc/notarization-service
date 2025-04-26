package eu.gaiax.notarization.api.profile

import eu.xfsc.not.api.util.JsonValueEnum
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    description = "Kind of credential specified for creation.",
)
enum class CredentialKind (override val value: String) : JsonValueEnum {
    AnonCred("AnonCred"),
    JsonLD("JSON-LD"),
    SD_JWT("SD-JWT"),
    ;

    fun asProfile(): AipVersion? {
        return when (this) {
            AnonCred -> AipVersion.V1_0
            JsonLD -> AipVersion.V2_0
            else -> null
        }
    }
}
