package eu.xfsc.not.oid4vp.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.xfsc.not.api.util.JsonValueEnum
import eu.xfsc.not.api.util.fromString
import jakarta.inject.Inject
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.ext.ParamConverter
import jakarta.ws.rs.ext.ParamConverterProvider
import jakarta.ws.rs.ext.Provider
import java.lang.reflect.Type


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class JarAuthRequest (

    /**
     * request REQUIRED unless request_uri is specified.
     * The Request Object (Section 2.1) that holds authorization request parameters stated in Section 4 of [RFC6749] (OAuth 2.0).
     * If this parameter is present in the authorization request, request_uri MUST NOT be present.
     */
    var request: String? = null,
    /**
     * request_uri REQUIRED unless request is specified.
     * The absolute URI, as defined by RFC 3986 [RFC3986], that is the Request Object URI (Section 2.2) referencing the authorization request parameters stated in Section 4 of [RFC6749] (OAuth 2.0).
     * If this parameter is present in the authorization request, request MUST NOT be present.
     */
    var requestUri: String? = null,
    /**
     * client_id REQUIRED.
     * OAuth 2.0 [RFC6749] client_id.
     * The value MUST match the request or request_uri Request Object's (Section 2.1) client_id.
     */
    var clientId: String,
)

fun JarAuthRequest.toQueryParams(om: ObjectMapper = jacksonObjectMapper()): Map<String, String> {
    val json = om.valueToTree<ObjectNode>(this)
    return json.fields().asSequence().associate {
        val value = it.value
        it.key to when (value) {
            is TextNode -> value.textValue()
            is NumericNode -> value.numberValue().toString()
            else -> om.writeValueAsString(value)
        }
    }
}



enum class ResponseType (override val value: String) : JsonValueEnum {
    NONE("none"),
    CODE("code"),
    TOKEN("token"),
    ID_TOKEN("id_token"),
    VP_TOKEN("vp_token"),
    ;
}

class ResponseTypeConverterDeser : StdConverter<String, List<ResponseType>>() {
    override fun convert(value: String?): List<ResponseType> {
        return value?.split(" ")?.mapNotNull { fromString(it) } ?: emptyList()
    }
}
class ResponseTypeConverterSer : StdConverter<List<ResponseType>, String?>() {
    override fun convert(value: List<ResponseType>): String? {
        return if (value.isEmpty()) {
            null
        } else {
            value.joinToString(" ") { it.value }
        }
    }
}


enum class ResponseMode (override val value: String) : JsonValueEnum {
    // OIDC
    QUERY("query"),
    FRAGMENT("fragment"),
    /**
     * direct_post:
     * In this mode, the Authorization Response is sent to the Verifier using an HTTPS POST request to an endpoint controlled by the Verifier.
     * The Authorization Response parameters are encoded in the body using the application/x-www-form-urlencoded content type.
     * The flow can end with an HTTPS POST request from the Wallet to the Verifier, or it can end with a redirect that follows the HTTPS POST request, if the Verifier responds with a redirect URI to the Wallet.
     */
    DIRECT_POST("direct_post"),
    ;
}


enum class ClientIdScheme (override val value: String) : JsonValueEnum {
    /**
     * pre-registered
     * This value represents the [RFC6749] default behavior, i.e., the Client Identifier needs to be known to the Wallet in advance of the Authorization Request.
     * The Verifier metadata is obtained using [RFC7591] or through out-of-band mechanisms.
     */
    PRE_REGISTERED("pre-registered"),
    /**
     * redirect_uri
     * This value indicates that the Verifier's redirect URI is also the value of the Client Identifier.
     * In this case, the Authorization Request MUST NOT be signed, the Verifier MAY omit the redirect_uri Authorization Request parameter, and all Verifier metadata parameters MUST be passed using the client_metadata or client_metadata_uri parameter defined in Section 5.
     */
    REDIRECT_URI("redirect_uri"),
    /**
     * entity_id
     * This value indicates that the Client Identifier is an Entity Identifier defined in OpenID Federation [OpenID.Federation].
     * Processing rules given in [OpenID.Federation] MUST be followed.
     * Automatic Registration as defined in [OpenID.Federation] MUST be used.
     * The Authorization Request MAY also contain a trust_chain parameter.
     * The final Verifier metadata is obtained from the Trust Chain after applying the policies, according to [OpenID.Federation].
     * The client_metadata or client_metadata_uri parameter, if present in the Authorization Request, MUST be ignored when this Client Identifier scheme is used.
     */
    ENTITY_ID("entity_id"),
    /**
     * did
     * This value indicates that the Client Identifier is a DID defined in [DID-Core].
     * The request MUST be signed with a private key associated with the DID.
     * A public key to verify the signature MUST be obtained from the verificationMethod property of a DID Document.
     * Since DID Document may include multiple public keys, a particular public key used to sign the request in question MUST be identified by the kid in the JOSE Header.
     * To obtain the DID Document, the Wallet MUST use DID Resolution defined by the DID method used by the Verifier.
     * All Verifier metadata other than the public key MUST be obtained from the client_metadata or the client_metadata_uri parameter as defined in Section 5.
     */
    DID("did"),
    /**
     * verifier_attestation
     * This Client Identifier Scheme allows the Verifier to authenticate using a JWT that is bound to a certain public key as defined in Section 10.
     * When the Client Identifier Scheme is verifier_attestation, the Client Identifier MUST equal the sub claim value in the Verifier attestation JWT.
     * The request MUST be signed with the private key corresponding to the public key in the cnf claim in the Verifier attestation JWT.
     * This serves as proof of possesion of this key.
     * The Verifier attestation JWT MUST be added to the jwt JOSE Header of the request object (see Section 10).
     * The Wallet MUST validate the signature on the Verifier attestation JWT.
     * The iss claim value of the Verifier Attestation JWT MUST identify a party the Wallet trusts for issuing Verifier Attestation JWTs.
     * If the Wallet cannot establish trust, it MUST refuse the request.
     * If the issuer of the Verifier Attestation JWT adds a redirect_uris claim to the attestation, the Wallet MUST ensure the redirect_uri request parameter value exactly matches one of the redirect_uris claim entries.
     * All Verifier metadata other than the public key MUST be obtained from the client_metadata or or the client_metadata_uri parameter.
     */
    VERIFIER_ATTESTATION("verifier_attestation"),
    /**
     * x509_san_dns
     * When the Client Identifier Scheme is x509_san_dns, the Client Identifier MUST be a DNS name and match a dNSName Subject Alternative Name (SAN) [RFC5280] entry in the leaf certificate passed with the request.
     * The request MUST be signed with the private key corresponding to the public key in the leaf X.509 certificate of the certificate chain added to the request in the x5c JOSE header [RFC7515] of the signed request object.
     * The Wallet MUST validate the signature and the trust chain of the X.509 certificate.
     * All Verifier metadata other than the public key MUST be obtained from the client_metadata parameter.
     * If the Wallet can establish trust in the Client Identifier authenticated through the certificate, e.g. because the Client Identifier is contained in a list of trusted Client Identifiers, it may allow the client to freely choose the redirect_uri value.
     * If not, the FQDN of the redirect_uri value MUST match the Client Identifier.
     */
    X509_SAN_DNS("x509_san_dns"),
    /**
     * x509_san_uri
     * When the Client Identifier Scheme is x509_san_uri, the Client Identifier MUST be a URI and match a uniformResourceIdentifier Subject Alternative Name (SAN) [RFC5280] entry in the leaf certificate passed with the request.
     * The request MUST be signed with the private key corresponding to the public key in the leaf X.509 certificate of the certificate chain added to the request in the x5c JOSE header [RFC7515] of the signed request object.
     * The Wallet MUST validate the signature and the trust chain of the X.509 certificate.
     * All Verifier metadata other than the public key MUST be obtained from the client_metadata parameter.
     * If the Wallet can establish trust in the Client Identifier authenticated through the certificate, e.g. because the Client Identifier is contained in a list of trusted Client Identifiers, it may allow the client to freely choose the redirect_uri value.
     * If not, the redirect_uri value MUST match the Client Identifier.
     */
    X509_SAN_URI("x509_san_uri");
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
open class AuthRequestObject (

    // OAuth 2.0

    /**
     * response_type REQUIRED.
     * Value MUST be set to "code".
     */
    @JsonSerialize(converter = ResponseTypeConverterSer::class)
    @JsonDeserialize(converter = ResponseTypeConverterDeser::class)
    var responseType: List<ResponseType>,
    /**
     * client_id REQUIRED.
     * The client identifier as described in Section 2.2.
     * The value MUST match the request or request_uri Request Object's (Section 2.1) client_id.
     */
    var clientId: String,
    /**
     * redirect_uri OPTIONAL.
     * As described in Section 3.1.2.
     */
    var redirectUri: String? = null,
    /**
     * scope OPTIONAL.
     * The scope of the access request as described by Section 3.3.
     */
    var scope: String? = null,
    /**
     * state RECOMMENDED.
     * An opaque value used by the client to maintain state between the request and callback.
     * The authorization server includes this value when redirecting the user-agent back to the client.
     * The parameter SHOULD be used for preventing cross-site request forgery as described in Section 10.12.
     */
    var state: String? = null,

    // OIDC

    /**
     * nonce: REQUIRED.
     * Defined in [OpenID.Core].
     * It is used to securely bind the Verifiable Presentation(s) provided by the Wallet to the particular transaction.
     * See Section 12.1 for details.
     */
    var nonce: String,

    // OID4VP

    /**
     * presentation_definition:
     * A string containing a Presentation Definition JSON object.
     * See Section 5.1 for more details.
     * This parameter MUST be present when presentation_definition_uri parameter, or a scope value representing a Presentation Definition is not present.
     */
    var presentationDefinition: PresentationDefinition? = null,
    /**
     * presentation_definition_uri:
     * A string containing an HTTPS URL pointing to a resource where a Presentation Definition JSON object can be retrieved.
     * This parameter MUST be present when presentation_definition parameter, or a scope value representing a Presentation Definition is not present.
     * See Section 5.2 for more details.
     */
    var presentationDefinitionUri: String? = null,
    /**
     * client_id_scheme: OPTIONAL.
     * A string identifying the scheme of the value in the client_id Authorization Request parameter (Client Identifier scheme).
     * The client_id_scheme parameter namespaces the respective Client Identifier.
     * If an Authorization Request uses the client_id_scheme parameter, the Wallet MUST interpret the Client Identifier of the Verifier in the context of the Client Identifier scheme.
     * If the parameter is not present, the Wallet MUST behave as specified in [RFC6749].
     * See Section 5.7 for the values defined by this specification.
     * If the same Client Identifier is used with different Client Identifier schemes, those occurrences MUST be treated as different Verifiers.
     * Note that the Verifier needs to determine which Client Identifier schemes the Wallet supports prior to sending the Authorization Request in order to choose a supported scheme.
     */
    var clientIdScheme: ClientIdScheme? = null,
    /**
     * client_metadata: OPTIONAL.
     * A JSON object containing the Verifier metadata values.
     * It MUST be UTF-8 encoded.
     * It MUST NOT be present if client_metadata_uri parameter is present.
     */
    var clientMetadata: Oid4VpClientMetadata? = null,
    /**
     * client_metadata_uri: OPTIONAL.
     * A string containing an HTTPS URL pointing to a resource where a JSON object with the Verifier metadata can be retrieved.
     * The scheme used in the client_metadata_uri value MUST be https.
     * The client_metadata_uri value MUST be reachable by the Wallet.
     * It MUST NOT be present if client_metadata parameter is present.
     */
    var clientMetadataUri: String? = null,

    /**
     * response_mode OPTIONAL.
     * Informs the Authorization Server of the mechanism to be used for returning Authorization Response parameters from the Authorization Endpoint.
     * This use of this parameter is NOT RECOMMENDED with a value that specifies the same Response Mode as the default Response Mode for the Response Type used.
     * @see <a href="https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html">OAuth 2.0 Multiple Response Type Encoding Practices, Sec. 2.1</a>
     */
    var responseMode: ResponseMode? = null,

    /**
     * response_uri: OPTIONAL.
     * MUST be present when the Response Mode direct_post is used.
     * The Response URI to which the Wallet MUST send the Authorization Response using an HTTPS POST request as defined by the Response Mode direct_post.
     * The Response URI receives all Authorization Response parameters as defined by the respective Response Type.
     * When the response_uri parameter is present, the redirect_uri Authorization Request parameter MUST NOT be present.
     * If the redirect_uri Authorization Request parameter is present when the Response Mode is direct_post, the Wallet MUST return an invalid_request Authorization Response error.
     */
    var responseUri: String? = null,
)




@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class AuthResponseRequest {
    /**
     * vp_token: REQUIRED.
     * JSON String or JSON object that MUST contain a single Verifiable Presentation or an array of JSON Strings and JSON objects each of them containing a Verifiable Presentations.
     * Each Verifiable Presentation MUST be represented as a JSON string (that is a Base64url encoded value) or a JSON object depending on a format as defined in Annex E of [OpenID.VCI].
     * When a single Verifiable Presentation is returned, the array syntax MUST NOT be used.
     * If Appendix E of [OpenID.VCI] defines a rule for encoding the respective Credential format in the Credential Response, this rules MUST also be followed when encoding Credentials of this format in the vp_token response parameter.
     * Otherwise, this specification does not require any additional encoding when a Credential format is already represented as a JSON object or a JSON string.
     */
    @FormParam("vp_token")
    lateinit var vpToken: JsonNode
    /**
     * presentation_submission: REQUIRED.
     * The presentation_submission element as defined in [DIF.PresentationExchange].
     * It contains mappings between the requested Verifiable Credentials and where to find them within the returned VP Token.
     * This is expressed via elements in the descriptor_map array, known as Input Descriptor Mapping Objects.
     * These objects contain a field called path, which, for this specification, MUST have the value $ (top level root path) when only one Verifiable Presentation is contained in the VP Token, and MUST have the value $[n] (indexed path from root) when there are multiple Verifiable Presentations, where n is the index to select.
     * The path_nested object inside an Input Descriptor Mapping Object is used to describe how to find a returned Credential within a Verifiable Presentation, and the value of the path field in it will ultimately depend on the credential format.
     * Non-normative examples can be found further in this section.
     */
    @FormParam("presentation_submission")
    lateinit var presentationSubmission: PresentationSubmission

    // other OAuth parameters
    @FormParam("state")
    var state: String? = null

    @FormParam("iss")
    var iss: String? = null
}

/**
 * Converter provider for the types in [AuthResponseRequest].
 */
@Provider
class AuthResponseRequestFormConverterProvider : ParamConverterProvider {
    @Inject
    lateinit var om: ObjectMapper
    override fun <T : Any?> getConverter(
        rawType: Class<T>?,
        genericType: Type?,
        annotations: Array<out Annotation>?
    ): ParamConverter<T>? {
        return if (rawType == PresentationSubmission::class.java) {
            object : ParamConverter<PresentationSubmission> {
                override fun fromString(value: String?): PresentationSubmission {
                    return om.readValue(value, PresentationSubmission::class.java)
                }

                override fun toString(value: PresentationSubmission): String {
                    return om.writeValueAsString(value)
                }
            } as ParamConverter<T>
        } else if (JsonNode::class.java.isAssignableFrom(rawType)) {
            object : ParamConverter<JsonNode> {
                override fun fromString(value: String?): JsonNode {
                    return om.readTree(value)
                }

                override fun toString(value: JsonNode): String {
                    return om.writeValueAsString(value)
                }
            } as ParamConverter<T>
        } else {
            return null
        }
    }
}


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class AuthResponseResponse (
    /**
     * redirect_uri: OPTIONAL.
     * When the redirect parameter is used the Wallet MUST send the User Agent to this redirect URI.
     * The redirect URI allows the Verifier to continue the interaction with the End-User on the device where the Wallet resides after the Wallet has sent the Authorization Response to the Response URI.
     * It especially enables the Verifier to prevent session fixation (Section 12.2) attacks.
     */
    var redirectUri: String? = null,
)


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class Oid4VpClientMetadata (
//    @JsonUnwrapped
//    var oauthMetadata: OauthProviderMetadata,
    /**
     *  vp_formats: REQUIRED.
     *  An object defining the formats and proof types of Verifiable Presentations and Verifiable Credentials that a Verifier supports.
     *  For specific values that can be used, see Appendix A.
     *  Deployments can extend the formats supported, provided Issuers, Holders and Verifiers all understand the new format.
     */
    var vpFormats: Map<VpFormatType, VpFormat>,
)

enum class VpFormatType (override val value: String) : JsonValueEnum {
    JWT_VP("jwt_vp_json"),
    LDP_VP("ldp_vp"),
    AC_VP("ac_vp"),
    MSO("mso_mdoc"),
    ;
}

open class VpFormat

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class VpFormatLdp (
    /**
     * List of proof algorithms.
     * @see <a href="https://identity.foundation/claim-format-registry/#registry">Claim Format Registry</a>
     */
    proofType: List<String>,
) : VpFormat()


/**
 * DIF Presentation Definition
 * @see <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#presentation-definition">DIF Presentation Exchange, Sec. 5</a>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class PresentationDefinition (
    /**
     * id - The Presentation Definition MUST contain an id property.
     * The value of this property MUST be a string.
     * The string SHOULD provide a unique ID for the desired context.
     * For example, a UUID such as 32f54163-7166-48f1-93d8-f f217bdb0653 could provide an ID that is unique in a global context, while a simple string such as my_presentation_definition_1 could be suitably unique in a local context.
     */
    var id: String,
    /**
     * input_descriptors - The Presentation Definition MUST contain an input_descriptors property.
     * Its value MUST be an array of Input Descriptor Objects, the composition of which are described in the Input Descriptors section below.
     *
     * All inputs listed in the input_descriptors array are required for submission, unless otherwise specified by a Feature.
     */
    var inputDescriptors: List<InputDescriptor>,
    /**
     * name - The Presentation Definition MAY contain a name property.
     * If present, its value SHOULD be a human-friendly string intended to constitute a distinctive designation of the Presentation Definition.
     */
    var name: String? = null,
    /**
     * purpose - The Presentation Definition MAY contain a purpose property.
     * If present, its value MUST be a string that describes the purpose for which the Presentation Definition's inputs are being used for.
     */
    var purpose: String? = null,
    /**
     * The Presentation Definition MAY include a format property.
     * Some envelope transport protocols may include the value of this property in other locations and use different property names (See the Format Embed Locations section for details), but regardless of whether it resides at the default location (the format property of the presentation_definition object), the value MUST be an object with one or more properties matching the registered Claim Format Designations (e.g., jwt, jwt_vc, jwt_vp, etc.).
     * The properties inform the Holder of the Claim format configurations the Verifier can process.
     * The value for each claim format property MUST be an object composed as follows:
     * * The object MUST include a format-specific property (i.e., alg, proof_type) that expresses which algorithms the Verifier supports for the format.
     *   Its value MUST be an array of one or more format-specific algorithmic identifier references, as noted in the Claim Format Designations section.
     */
    var format: Map<VpFormatType, VpFormat>
)

/**
 * DIF Input Descriptor
 * @see <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#input-descriptor">DIF Presentation Exchange, Sec. 5.1</a>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class InputDescriptor (
    /**
     * The Input Descriptor Object MUST contain an id property.
     * The value of the id property MUST be a string that does not conflict with the id of another Input Descriptor Object in the same Presentation Definition.
     */
    var id: String,
    /**
     * The Input Descriptor Object MAY contain a name property.
     * If present, its value SHOULD be a human-friendly name that describes what the target schema represents.
     */
    var name: String? = null,
    /**
     * The Input Descriptor Object MAY contain a purpose property.
     * If present, its value MUST be a string that describes the purpose for which the Claim's data is being requested.
     */
    var purpose: String? = null,
    /**
     * The Input Descriptor Object MAY contain a format property.
     * If present, its value MUST be an object with one or more properties matching the registered Claim Format Designations (e.g., jwt, jwt_vc, jwt_vp, etc.).
     * This format property is identical in value signature to the top-level format object, but can be used to specifically constrain submission of a single input to a subset of formats or algorithms.
     */
    var format: Map<VpFormatType, VpFormat>? = null,
    /**
     * The Input Descriptor Object MUST contain a constraints property.
     */
    var constraints: ConstraintsObject,
)

/**
 * DIF Input Descriptor Constraints Object
 * @see <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#input-descriptor">DIF Presentation Exchange, Sec. 5.1</a>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ConstraintsObject (
    /**
     * The constraints object MAY contain a fields property.
     * Fields SHALL be processed forward from 0-index, so if a Verifier desires to reduce processing by checking the most defining characteristics of a credential (e.g the type or schema of a credential) implementers SHOULD order these field checks before all others to ensure earliest termination of evaluation.
     * If the fields property is present, its value MUST be an array of objects composed as follows, unless otherwise specified by a feature:
     */
    var fields: List<Field> = listOf(),
    /**
     * The constraints object MAY contain a limit_disclosure property.
     * If present, its value MUST be one of the following strings:
     * * required - This indicates that the Conformant Consumer MUST limit submitted fields to those listed in the fields array (if present).
     *   Conformant Consumers are not required to implement support for this value, but they MUST understand this value sufficiently to return nothing (or cease the interaction with the Verifier) if they do not implement it.
     * * preferred - This indicates that the Conformant Consumer SHOULD limit submitted fields to those listed in the fields array (if present).
     *
     * Omission of the limit_disclosure property indicates the Conformant Consumer MAY submit a response that contains more than the data described in the fields array.
     */
    var limitDisclosure: LimitedDisclosure = LimitedDisclosure.PREFERRED,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class Field (
    /**
     * The fields object MUST contain a path property.
     * The value of this property MUST be an array of one or more JSONPath string expressions (as defined in the JSONPath Syntax Definition section) that select a target value from the input.
     * The array MUST be evaluated from 0-index forward, breaking as soon as a Field Query Result is found (as described in Input Evaluation), which will be used for the rest of the entry’s evaluation.
     * The ability to declare multiple expressions in this way allows the Verifier to account for format differences - for example:
     * normalizing the differences in structure between JSON-LD/JWT-based Verifiable Credentials and vanilla JSON Web Tokens (JWTs) [RFC7519].
     */
    var path: List<String>,
    /**
     * The fields object MAY contain an id property.
     * If present, its value MUST be a string that is unique from every other field object’s id property, including those contained in other Input Descriptor Objects.
     */
    var id: String? = null,
    /**
     * The fields object MAY contain a purpose property.
     * If present, its value MUST be a string that describes the purpose for which the field is being requested.
     */
    var purpose: String? = null,
    /**
     * The fields object MAY contain a name property.
     * If present, its value MUST be a string, and SHOULD be a human-friendly name that describes what the target field represents.
     */
    var name: String? = null,
    /**
     * The fields object MAY contain a filter property, and if present its value MUST be a JSON Schema descriptor used to filter against the values returned from evaluation of the JSONPath string expressions in the path array.
     */
    var filter: JsonNode? = null,
    /**
     * The fields object MAY contain an optional property.
     * The value of this property MUST be a boolean, wherein true indicates the field is optional, and false or non-presence of the property indicates the field is required.
     * Even when the optional property is present, the value located at the indicated path of the field MUST validate against the JSON Schema filter, if a filter is present.
     */
    var optional: Boolean = false,
)

enum class LimitedDisclosure (override val value: String) : JsonValueEnum {
    REQUIRED("required"),
    PREFERRED("preferred"),
    ;
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class PresentationSubmission (
    /**
     * The presentation_submission object MUST contain an id property.
     * The value of this property MUST be a unique identifier, such as a UUID.
     */
    var id: String,
    /**
     * The presentation_submission object MUST contain a definition_id property.
     * The value of this property MUST be the id value of a valid Presentation Definition.
     */
    var definitionId: String,
    /**
     * The presentation_submission object MUST include a descriptor_map property.
     * The value of this property MUST be an array of Input Descriptor Mapping Objects, composed as follows:
     */
    var descriptorMap: List<DescriptorMapObject>,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class DescriptorMapObject (
    /**
     * The descriptor_map object MUST include an id property.
     * The value of this property MUST be a string that matches the id property of the Input Descriptor in the Presentation Definition that this Presentation Submission is related to.
     */
    var id: String,
    /**
     * The descriptor_map object MUST include a format property.
     * The value of this property MUST be a string that matches one of the Claim Format Designation. This denotes the data format of the Claim.
     */
    var format: VpFormatType,
)
