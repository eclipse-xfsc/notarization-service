package eu.xfsc.not.vc

import com.danubetech.keyformats.jose.JWK
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import id.walt.crypto.keys.Key
import id.walt.crypto.keys.jwk.JWKKey
import id.walt.did.utils.KeyUtils
import kotlinx.coroutines.runBlocking

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class DidDocument (
    @JsonAnyGetter
    @JsonAnySetter
    var unknown: MutableMap<String, Any> = mutableMapOf(),
    /**
     * id: REQUIRED
     * A string that conforms to the rules in 3.1 DID Syntax.
     */
    var id: String,
    /**
     * alsoKnownAs: OPTIONAL
     * A set of strings that conform to the rules of [RFC3986] for URIs.
     */
    var alsoKnownAs: List<String> = listOf(),
    /**
     * controller: OPTIONAL
     * A string that conforms to the rules in 3.1 DID Syntax.
     */
    var controller: String? = null,
    /**
     * verificationMethod: OPTIONAL
     * A set of Verification Method maps that conform to the rules in Verification Method properties.
     */
    var verificationMethod: List<VerificationMethod> = listOf(),
    /**
     * authentication: OPTIONAL
     * A set of either Verification Method maps that conform to the rules in Verification Method properties) or strings that conform to the rules in 3.2 DID URL Syntax.
     */
    @JsonDeserialize(contentConverter = VerificationMethodOrDidConverter::class)
    var authentication: List<VerificationMethodOrDid> = listOf(),
    /**
     * assertionMethod: OPTIONAL
     * A set of either Verification Method maps that conform to the rules in Verification Method properties) or strings that conform to the rules in 3.2 DID URL Syntax.
     */
    @JsonDeserialize(contentConverter = VerificationMethodOrDidConverter::class)
    var assertionMethod: List<VerificationMethodOrDid> = listOf(),
    /**
     * keyAgreement: OPTIONAL
     * A set of either Verification Method maps that conform to the rules in Verification Method properties) or strings that conform to the rules in 3.2 DID URL Syntax.
     */
    @JsonDeserialize(contentConverter = VerificationMethodOrDidConverter::class)
    var keyAgreement: List<VerificationMethodOrDid> = listOf(),
    /**
     * capabilityInvocation: OPTIONAL
     * A set of either Verification Method maps that conform to the rules in Verification Method properties) or strings that conform to the rules in 3.2 DID URL Syntax.
     */
    @JsonDeserialize(contentConverter = VerificationMethodOrDidConverter::class)
    var capabilityInvocation: List<VerificationMethodOrDid> = listOf(),
    /**
     * capabilityDelegation: OPTIONAL
     * A set of either Verification Method maps that conform to the rules in Verification Method properties) or strings that conform to the rules in 3.2 DID URL Syntax.
     */
    @JsonDeserialize(contentConverter = VerificationMethodOrDidConverter::class)
    var capabilityDelegation: List<VerificationMethodOrDid> = listOf(),
    /**
     * service: OPTIONAL
     * A set of Service Endpoint maps that conform to the rules in Service properties.
     */
    var service: List<ServiceEndpoint> = listOf()
)

class VerificationMethodOrDidConverter : StdConverter<JsonNode, VerificationMethodOrDid>() {
    override fun convert(value: JsonNode): VerificationMethodOrDid {
        return if (value.isTextual) {
            DidUri(value.asText())
        } else {
            jacksonObjectMapper().treeToValue(value, VerificationMethod::class.java)
        }
    }
}


interface VerificationMethodOrDid

class DidUri(
    @JsonValue
    var value: String
) : VerificationMethodOrDid


@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class VerificationMethod (
    /**
     * id: REQUIRED
     * A string that conforms to the rules in 3.2 DID URL Syntax.
     */
    var id: String,
    /**
     * controller: REQUIRED
     * A string that conforms to the rules in 3.1 DID Syntax.
     */
    var controller: String,
    /**
     * type: REQUIRED
     * A string.
     */
    var type: String,
    /**
     * publicKeyJwk: OPTIONAL
     * A map representing a JSON Web Key that conforms to [RFC7517].
     * See definition of publicKeyJwk for additional constraints.
     */
    var publicKeyJwk: ObjectNode? = null,
    /**
     * publicKeyMultibase: OPTIONAL
     * A string that conforms to a [MULTIBASE] encoded public key.
     */
    var publicKeyMultibase: String? = null,
) : VerificationMethodOrDid {
    fun extractPublicKey(): Result<Key> = runBlocking {
        if (publicKeyJwk != null) {
            val jwkString = jacksonObjectMapper().writeValueAsString(publicKeyJwk)
            JWKKey.importJWK(jwkString)
        } else if (publicKeyMultibase != null) {
            KeyUtils.fromPublicKeyMultiBase(publicKeyMultibase!!)
        } else {
            Result.failure(IllegalStateException("No public key found"))
        }
    }

    fun extractPublicKeyJwk(): Result<JWK> = runBlocking {
        if (publicKeyJwk != null) {
            runCatching {
                val jwkString = jacksonObjectMapper().writeValueAsString(publicKeyJwk)
                JWK.fromJson(jwkString)
            }
        } else if (publicKeyMultibase != null) {
            val key = KeyUtils.fromPublicKeyMultiBase(publicKeyMultibase!!)
            key.map { JWK.fromJson(it.exportJWK()) }
        } else {
            Result.failure(IllegalStateException("No public key found"))
        }
    }
}

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ServiceEndpoint (
    /**
     * id: REQUIRED
     * A string that conforms to the rules of [RFC3986] for URIs.
     */
    var id: String,
    /**
     * type: REQUIRED
     * A string or a set of strings.
     */
    @JsonDeserialize(converter = ServiceTypeConverter::class)
    var type: List<String>,
    /**
     * serviceEndpoint: REQUIRED
     * A string that conforms to the rules of [RFC3986] for URIs, a map, or a set composed of a one or more strings that conform to the rules of [RFC3986] for URIs and/or maps.
     */
    var serviceEndpoint: JsonNode,
)


class ServiceTypeConverter : StdConverter<JsonNode, List<String>>() {
    override fun convert(value: JsonNode): List<String> {
        return if (value.isTextual) {
            listOf(value.asText())
        } else if (value.isArray) {
            val values: List<String> = jacksonObjectMapper().valueToTree(value)
            values
        } else {
            listOf()
        }
    }
}
