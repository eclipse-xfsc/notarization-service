package eu.gaiax.notarization.profile.infrastructure.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ProfileIssuanceSpec
import eu.gaiax.notarization.api.issuance.SignatureType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    description = "Kind of DID used for the profile.",
    enumeration = [
        DidType.Name.Automatic,
        DidType.Name.Provided
    ]
)
enum class DidType (private val value: String) {
    Automatic(Name.Automatic),
    Provided(Name.Provided);

    @JsonValue
    override fun toString(): String {
        return value
    }
    object Name {
        const val Automatic = "automatic"
        const val Provided = "provided"
    }

    companion object {
        fun fromString(s: String): DidType {
            if (s == Name.Automatic) {
                return Automatic
            } else if (s == Name.Provided) {
                return Provided
            }
            throw IllegalArgumentException("The following value is not a valid AIP version specifier: $s")
        }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(value = [
    JsonSubTypes.Type(value = AutomaticDidCreationRequest::class, name = "automatic"),
    JsonSubTypes.Type(value = ProvidedDidRequest::class, name = "provided")
])
abstract class ProfileDidRequest {

}

@Schema(
    description = "The parameters are used to create new keys for issuance for the given profile."
)
class AutomaticDidCreationRequest: ProfileDidRequest() {

    val versions: Set<ApiVersion>? = null
    @get:Schema(description = "The type of key of the new signing key for the issuance v2.")
    val keyType: KeyType? = null
    @get:Schema(description = "The type of signature of the new signing key for the issuance v2.")
    val signatureType: SignatureType? = null
}

class ProvidedDidRequest: ProfileDidRequest() {
    @get:Schema(
        description = "Specifies the did parameters to use when issuing a VC for the profile with the issuance service v1. It must be available to the issuance service v1.",
        oneOf = [
            Aip10IssuanceSpecification::class,
            Aip20IssuanceSpecification::class
        ]
    )
    var v1: JsonNode? = null
    @get:Schema(
        description = "Specifies the issuing did and revocating did to use when issuing a VC for the profile with the issuance service v2. It must be available to the issuance service v2.",
    )
    var v2: ProfileIssuanceSpec? = null
}

class Aip10IssuanceSpecification(
    val did: String,
    val schemaId: String,
    val credentialDefinitionId: String) {
}
class Aip20IssuanceSpecification(
    val issuingDid: String,
    val revocatingDid: String?
)
