package eu.gaiax.notarization.api.issuance

import com.fasterxml.jackson.annotation.JsonValue
import eu.gaiax.notarization.api.profile.CredentialKind
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    description = "The version of the SSI Issuance service used within the Notarization-AP.",
    enumeration = [
        ApiVersion.Name.V1,
        ApiVersion.Name.V2
    ]
)
enum class ApiVersion (private val value: String) {
    V1(Name.V1),
    V2(Name.V2);

    @JsonValue
    override fun toString(): String {
        return value
    }

    object Name {
        const val V1 = "v1"
        const val V2 = "v2"
    }

    fun supportsCredentialKind(kind: CredentialKind?): Boolean {
        return supportsKind[this]?.contains(kind) ?: false
    }

    companion object {
        fun fromString(s: String): ApiVersion {
            if (s == Name.V1) {
                return V1
            } else if (s == Name.V2) {
                return V2
            }
            throw IllegalArgumentException("The following value is not a valid SSI API version specifier: $s")
        }

        val supportsKind = mapOf<ApiVersion, Set<CredentialKind>>(
            V1 to setOf(CredentialKind.AnonCred, CredentialKind.JsonLD),
            V2 to setOf(CredentialKind.JsonLD, CredentialKind.SD_JWT)
        )
    }

}
