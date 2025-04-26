package eu.xfsc.not.api.oid4vci.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer
import com.fasterxml.jackson.databind.jsontype.*
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.kotlin.contains
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.xfsc.not.api.util.JsonValueEnum
import eu.xfsc.not.api.util.fromString
import jakarta.ws.rs.FormParam
import java.io.IOException
import java.net.URLEncoder


// All based on OpenID for Verifiable Credential Issuance - draft 13
// https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class CredentialOffer (
    /**
     * credential_issuer: REQUIRED.
     * The URL of the Credential Issuer, as defined in Section 11.2.1, from which the Wallet is requested to obtain one or more Credentials.
     * The Wallet uses it to obtain the Credential Issuer's Metadata following the steps defined in Section 11.2.2.
     */
    var credentialIssuer: String,
    /**
     * credential_configuration_ids: REQUIRED.
     * Array of unique strings that each identify one of the keys in the name/value pairs stored in the credential_configurations_supported Credential Issuer metadata.
     * The Wallet uses these string values to obtain the respective object that contains information about the Credential being offered as defined in Section 11.2.3.
     * For example, these string values can be used to obtain scope values to be used in the Authorization Request.
     */
    var credentialConfigurationIds: List<String>,
    /**
     * grants: OPTIONAL.
     * Object indicating to the Wallet the Grant Types the Credential Issuer's Authorization Server is prepared to process for this Credential Offer.
     * Every grant is represented by a name/value pair.
     * The name is the Grant Type identifier; the value is an object that contains parameters either determining the way the Wallet MUST use the particular grant and/or parameters the Wallet MUST send with the respective request(s).
     * If grants is not present or is empty, the Wallet MUST determine the Grant Types the Credential Issuer's Authorization Server supports using the respective metadata.
     * When multiple grants are present, it is at the Wallet's discretion which one to use.
     */
    var grants: OfferGrants? = null,
)

fun CredentialOffer.toOfferByValueUri(om: ObjectMapper = jacksonObjectMapper()) : String {
    val objStr = om.writeValueAsString(this)
    val objStrEnc = URLEncoder.encode(objStr, Charsets.US_ASCII)
    return "openid-credential-offer://?credential_offer=$objStrEnc"
}

object GrantTypes {
    const val CodeName = "authorization_code"
    const val Implicit = "implicit"
    const val Password = "password"
    const val ClientCredentials = "client_credentials"
    const val RefreshToken = "refresh_token"
    const val JwtBearer = "urn:ietf:params:oauth:grant-type:jwt-bearer"
    const val SamlBearer = "urn:ietf:params:oauth:grant-type:saml2-bearer"
    const val PreAuthCodeName = "urn:ietf:params:oauth:grant-type:pre-authorized_code"
}

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class OfferGrants(
    @get:JsonProperty(GrantTypes.CodeName)
    var codeGrantOffer: CodeGrantOffer? = null,
    @get:JsonProperty(GrantTypes.PreAuthCodeName)
    var preAuthCodeGrantOffer: PreAuthCodeGrantOffer? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class CodeGrantOffer(
    /**
     * issuer_state: OPTIONAL.
     * String value created by the Credential Issuer and opaque to the Wallet that is used to bind the subsequent Authorization Request with the Credential Issuer to a context set up during previous steps.
     * If the Wallet decides to use the Authorization Code Flow and received a value for this parameter, it MUST include it in the subsequent Authorization Request to the Credential Issuer as the issuer_state parameter value.
     */
    var issuerState: String? = null,
    /**
     * authorization_server: OPTIONAL
     * string that the Wallet can use to identify the Authorization Server to use with this grant type when authorization_servers parameter in the Credential Issuer metadata has multiple entries.
     * It MUST NOT be used otherwise.
     * The value of this parameter MUST match with one of the values in the authorization_servers array obtained from the Credential Issuer metadata.
     */
    var authorizationServer: String? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class PreAuthCodeGrantOffer(
    /**
     * pre-authorized_code: REQUIRED.
     * The code representing the Credential Issuer's authorization for the Wallet to obtain Credentials of a certain type.
     * This code MUST be short lived and single use.
     * If the Wallet decides to use the Pre-Authorized Code Flow, this parameter value MUST be included in the subsequent Token Request with the Pre-Authorized Code Flow.
     */
    @JsonProperty("pre-authorized_code")
    var preAuthorizedCode: String,
    /**
     * tx_code: OPTIONAL.
     * Object specifying whether the Authorization Server expects presentation of a Transaction Code by the End-User along with the Token Request in a Pre-Authorized Code Flow.
     * If the Authorization Server does not expect a Transaction Code, this object is absent; this is the default.
     * The Transaction Code is intended to bind the Pre-Authorized Code to a certain transaction to prevent replay of this code by an attacker that, for example, scanned the QR code while standing behind the legitimate End-User.
     * It is RECOMMENDED to send the Transaction Code via a separate channel.
     * If the Wallet decides to use the Pre-Authorized Code Flow, the Transaction Code value MUST be sent in the tx_code parameter with the respective Token Request as defined in Section 6.1.
     * If no length or description is given, this object may be empty, indicating that a Transaction Code is required.
     */
    var txCode: TxCode? = null,
    /**
     * interval: OPTIONAL.
     * The minimum amount of time in seconds that the Wallet SHOULD wait between polling requests to the token endpoint (in case the Authorization Server responds with error code authorization_pending - see Section 6.3).
     * If no value is provided, Wallets MUST use 5 as the default.
     */
    var interval: Int? = 5,
    /**
     * authorization_server: OPTIONAL
     * string that the Wallet can use to identify the Authorization Server to use with this grant type when authorization_servers parameter in the Credential Issuer metadata has multiple entries.
     * It MUST NOT be used otherwise.
     * The value of this parameter MUST match with one of the values in the authorization_servers array obtained from the Credential Issuer metadata.
     */
    var authorizationServer: String? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class TxCode(
    /**
     * input_mode : OPTIONAL.
     * String specifying the input character set.
     * Possible values are numeric (only digits) and text (any characters).
     * The default is numeric.
     */
    var inputMode: String? = "numeric",
    /**
     * length: OPTIONAL.
     * Integer specifying the length of the Transaction Code.
     * This helps the Wallet to render the input screen and improve the user experience.
     */
    var length: Int? = null,
    /**
     * description: OPTIONAL.
     * String containing guidance for the Holder of the Wallet on how to obtain the Transaction Code, e.g., describing over which communication channel it is delivered.
     * The Wallet is RECOMMENDED to display this description next to the Transaction Code input screen to improve the user experience.
     * The length of the string MUST NOT exceed 300 characters.
     * The description does not support internationalization, however the Issuer MAY detect the Holder's language by previous communication or an HTTP Accept-Language header within an HTTP GET request for a Credential Offer URI.
     */
    var description: String? = null,
)


open class Oid4VciTokenRequest : OauthTokenRequest() {
    /**
     * pre-authorized_code:
     * The code representing the authorization to obtain Credentials of a certain type.
     * This parameter MUST be present if the grant_type is urn:ietf:params:oauth:grant-type:pre-authorized_code.
     */
    @FormParam("pre-authorized_code")
    var preAuthorizedCode: String? = null

    /**
     * tx_code: OPTIONAL.
     * String value containing a Transaction Code.
     * This value MUST be present if a tx_code object was present in the Credential Offer (including if the object was empty).
     * This parameter MUST only be used if the grant_type is urn:ietf:params:oauth:grant-type:pre-authorized_code.
     */
    @FormParam("tx_code")
    var txCode: String? = null
}

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Oid4VciTokenResponse (
    @get:JsonUnwrapped
    val oauthTokenResponse: OauthTokenResponse,

    /**
     * c_nonce: OPTIONAL.
     * String containing a nonce to be used when creating a proof of possession of the key proof (see Section 7.2).
     * When received, the Wallet MUST use this nonce value for its subsequent requests until the Credential Issuer provides a fresh nonce.
     */
    @get:JsonProperty("c_nonce")
    var cNonce: String? = null,
    /**
     * c_nonce_expires_in: OPTIONAL.
     * Number denoting the lifetime in seconds of the c_nonce.
     */
    @get:JsonProperty("c_nonce_expires_in")
    var cNonceExpiresIn: Long? = null,
    /**
     * authorization_details: REQUIRED when authorization_details parameter is used to request issuance of a certain Credential type as defined in Section 5.1.1.
     * It MUST NOT be used otherwise.
     * It is an array of objects, as defined in Section 7 of [RFC9396].
     * In addition to the parameters defined in Section 5.1.1, this specification defines the following parameter to be used with the authorization details type openid_credential in the Token Response:
     * credential_identifiers: OPTIONAL.
     * Array of strings, each uniquely identifying a Credential that can be issued using the Access Token returned in this response.
     * Each of these Credentials corresponds to the same entry in the credential_configurations_supported Credential Issuer metadata but can contain different claim values or a different subset of claims within the claims set identified by that Credential type.
     * This parameter can be used to simplify the Credential Request, as defined in Section 7.2, where the credential_identifier parameter replaces the format parameter and any other Credential format-specific parameters in the Credential Request.
     * When received, the Wallet MUST use these values together with an Access Token in subsequent Credential Requests.
     */
    var authorizationDetails: List<AuthorizationDetails> = emptyList(),
)


private class AuthorizationDetailsDeserializer : StdNodeBasedDeserializer<AuthorizationDetails>(AuthorizationDetails::class.java) {
    override fun convert(root: JsonNode, ctxt: DeserializationContext): AuthorizationDetails {
        val p = ctxt.parser
        return if (root.contains("format")) {
            p.codec.treeToValue(root, SpecifyingAuthorizationDetails::class.java)
        } else if (root.contains("credential_configuration_id")) {
            p.codec.treeToValue(root, ReferencedAuthorizationDetails::class.java)
        } else {
            throw IOException("AuthorizationDetails type is missing know discriminator value.")
        }
    }
}

@JsonDeserialize(using = AuthorizationDetailsDeserializer::class)
@JsonNaming(SnakeCaseStrategy::class)
open class AuthorizationDetails {
    /**
     * type: REQUIRED
     * String that determines the authorization details type.
     * It MUST be set to openid_credential for the purpose of this specification.
     */
    var type: String = "openid_credential"
    /**
     * credential_identifiers: OPTIONAL
     * Array of strings, each uniquely identifying a Credential that can be issued using the Access Token returned in this response.
     * Each of these Credentials corresponds to the same entry in the credential_configurations_supported Credential Issuer metadata but can contain different claim values or a different subset of claims within the claims set identified by that Credential type.
     * This parameter can be used to simplify the Credential Request, as defined in Section 7.2, where the credential_identifier parameter replaces the format parameter and any other Credential format-specific parameters in the Credential Request.
     * When received, the Wallet MUST use these values together with an Access Token in subsequent Credential Requests.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    var credentialIdentifiers: List<String> = emptyList()
}

@JsonDeserialize(using = JsonDeserializer.None::class)
class ReferencedAuthorizationDetails(
    /**
     * credential_configuration_id: REQUIRED when format parameter is not present
     * String specifying a unique identifier of the Credential being described in the credential_configurations_supported map in the Credential Issuer Metadata as defined in Section 11.2.3.
     * The referenced object in the credential_configurations_supported map conveys the details, such as the format, for issuance of the requested Credential.
     * This specification defines Credential Format specific Issuer Metadata in Appendix A.
     * It MUST NOT be present if format parameter is present.
     */
    val credentialConfigurationId: String
) : AuthorizationDetails()

@JsonDeserialize(using = JsonDeserializer.None::class)
class SpecifyingAuthorizationDetails : AuthorizationDetails() {
    /**
     * format: REQUIRED when credential_configuration_id parameter is not present
     * String specifying the format of the Credential to be issued.
     * Credential Format Profiles consist of the Credential format specific parameters that are defined in Appendix A.
     * It MUST NOT be present if credential_configuration_id parameter is present.
     */
    @JsonUnwrapped
    lateinit var credConfig: CredentialConfigurationSupported
}


private class CredRequestDeserializer : StdNodeBasedDeserializer<CredentialRequest>(CredentialRequest::class.java) {

    override fun convert(root: JsonNode, ctxt: DeserializationContext): CredentialRequest {
        val p = ctxt.parser
        return if (root.contains("format")) {
            val sub = p.codec.treeToValue(root, SpecifyingCredentialRequest::class.java)
            sub.ext = buildExt(root, p)
            sub
        } else {
            p.codec.treeToValue(root, ReferencedCredentialRequest::class.java)
        }
    }

    @Throws(IllegalArgumentException::class)
    fun buildExt(n: JsonNode, p: JsonParser) : CredentialExtension {
        val format = n.get("format").asText()
        val formatEnum: CredentialFormatEnum? = fromString(format)
        return if (formatEnum == null) {
            throw IllegalArgumentException("Unknown format: $format")
        } else {
            val extClazz = when (formatEnum) {
                CredentialFormatEnum.VC_JWT_JSON, CredentialFormatEnum.VC_JWT_JSON_LD, CredentialFormatEnum.VC_LDP_JSON_LD -> {
                    W3cCredentialRequestExtension::class.java
                }
                CredentialFormatEnum.VC_SD_JWT -> {
                    SdJwtCredentialRequestExtension::class.java
                }
                CredentialFormatEnum.MSO_MDOC -> {
                    SdJwtCredentialRequestExtension::class.java
                }
            }
            p.codec.treeToValue(n, extClazz)
        }
    }
}

@JsonDeserialize(using = CredRequestDeserializer::class)
@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
abstract class CredentialRequest {
    /**
     * proof: OPTIONAL. Object containing the proof of possession of the cryptographic key material the issued Credential would be bound to.
     * The proof object is REQUIRED if the proof_types_supported parameter is non-empty and present in the credential_configurations_supported parameter of the Issuer metadata for the requested Credential.
     * The proof object MUST contain the following:
     * <ul>
     *   <li>
     *     <code>proof_type</code>: REQUIRED.<br>
     *     String denoting the key proof type.
     *     The value of this parameter determines other parameters in the key proof object and its respective processing rules.
     *     Key proof types defined in this specification can be found in Section 7.2.1.
     *   </li>
     * </ul>
     */
    var proof: Proof? = null

    /**
     * credential_response_encryption: OPTIONAL.
     * Object containing information for encrypting the Credential Response.
     * If this request element is not present, the corresponding credential response returned is not encrypted.
     */
    var credentialResponseEncryption: CredentialResponseEncryption? = null
}


object CredentialRequestError {
    /**
     * invalid_credential_request:
     * The Credential Request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the same parameter, or is otherwise malformed.
     */
    const val InvalidCredentialRequest = "invalid_credential_request"
    /**
     * unsupported_credential_type:
     * Requested Credential type is not supported.
     */
    const val UnsupportedCredentialType = "unsupported_credential_type"
    /**
     * unsupported_credential_format:
     * Requested Credential format is not supported.
     */
    const val UnsupportedCredentialFormat = "unsupported_credential_format"
    /**
     * invalid_proof:
     * The proof in the Credential Request is invalid.
     * The proof field is not present or the provided key proof is invalid or not bound to a nonce provided by the Credential Issuer.
     */
    const val InvalidProof = "invalid_proof"
    /**
     * invalid_encryption_parameters:
     * This error occurs when the encryption parameters in the Credential Request are either invalid or missing.
     * In the latter case, it indicates that the Credential Issuer requires the Credential Response to be sent encrypted, but the Credential Request does not contain the necessary encryption parameters.
     */
    const val InvalidEncryptionParameters = "invalid_encryption_parameters"
}


@JsonNaming(SnakeCaseStrategy::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
class ReferencedCredentialRequest (
    /**
     * credential_identifier: REQUIRED when credential_identifiers parameter was returned from the Token Response.
     * It MUST NOT be used otherwise.
     * It is a String that identifies a Credential that is being requested to be issued.
     * When this parameter is used, the format parameter and any other Credential format specific parameters such as those defined in Appendix A MUST NOT be present.
     */
    val credentialIdentifier: String
) : CredentialRequest()

@JsonDeserialize(using = JsonDeserializer.None::class)
class SpecifyingCredentialRequest () : CredentialRequest() {
    @get:JsonUnwrapped
    lateinit var ext: CredentialExtension
}


@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown=true)
open class CredentialExtension {
    /**
     * format: REQUIRED when the credential_identifiers parameter was not returned from the Token Response.
     * It MUST NOT be used otherwise.
     * It is a String that determines the format of the Credential to be issued, which may determine the type and any other information related to the Credential to be issued.
     * Credential Format Profiles consist of the Credential format specific parameters that are defined in Appendix A.
     * When this parameter is used, the credential_identifier Credential Request parameter MUST NOT be present.
     */
    lateinit var format: CredentialFormatEnum

    companion object {
        fun JwtJson() = W3cCredentialRequestExtension().apply { format = CredentialFormatEnum.VC_JWT_JSON }
        fun JwtJsonLd() = W3cCredentialRequestExtension().apply { format = CredentialFormatEnum.VC_JWT_JSON_LD }
        fun LdpJsonLd() = W3cCredentialRequestExtension().apply { format = CredentialFormatEnum.VC_LDP_JSON_LD }
        fun IsoMDl() = MDlCredentialRequestExtension().apply { format = CredentialFormatEnum.MSO_MDOC }
        fun SdJwt() = SdJwtCredentialRequestExtension().apply { format = CredentialFormatEnum.VC_SD_JWT }
    }
}

class W3cCredentialRequestExtension internal constructor() : CredentialExtension() {
    /**
     * credential_definition: REQUIRED when the format parameter is present in the Credential Request.
     * It MUST NOT be used otherwise.
     * It is an object containing the detailed description of the Credential type.
     */
    lateinit var credentialDefinition: ObjectNode
}

class MDlCredentialRequestExtension internal constructor() : CredentialExtension() {
    /**
     * doctype: REQUIRED when the format parameter is present in the Credential Request.
     * It MUST NOT be used otherwise.
     * It is a string as defined in Appendix A.2.2.
     * The Credential issued by the Credential Issuer MUST contain at least the values listed in this claim.
     */
    lateinit var doctype: String
    /**
     * claims: OPTIONAL. Object as defined in Appendix A.2.2.
     */
    var claims: ClaimTree? = null
}

class SdJwtCredentialRequestExtension internal constructor() : CredentialExtension() {
    /**
     * vct: REQUIRED when the format parameter is present in the Credential Request.
     * It MUST NOT be used otherwise.
     * It is a string as defined in Appendix A.3.2.
     * This claim contains the type value of the Credential that the Wallet requests the Credential Issuer to issue.
     */
    lateinit var vct: String
    /**
     * claims: OPTIONAL. Object as defined in Appendix A.3.2.
     */
    var claims: ClaimTree? = null
}



@JsonTypeInfo(
    use =  JsonTypeInfo.Id.DEDUCTION,
)
@JsonSubTypes(
    Type(CredentialResponseSync::class),
    Type(CredentialResponseAsync::class),
)
@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
abstract class CredentialResponse (
    /**
     * c_nonce: OPTIONAL. String containing a nonce to be used to create a proof of possession of key material when requesting a Credential (see Section 7.2).
     * When received, the Wallet MUST use this nonce value for its subsequent Credential Requests until the Credential Issuer provides a fresh nonce.
     */
    @get:JsonProperty("c_nonce")
    var cNonce: String? = null,

    /**
     * c_nonce_expires_in: OPTIONAL.
     * Number denoting the lifetime in seconds of the c_nonce.
     */
    @get:JsonProperty("c_nonce_expires_in")
    var cNonceExpiresIn: Long? = null,

    /**
     * notification_id: OPTIONAL. String identifying an issued Credential that the Wallet includes in the Notification Request as defined in Section 10.1.
     * This parameter MUST NOT be present if credential parameter is not present.
     */
    var notificationId: String? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class CredentialResponseSync (
    /**
     * credential: OPTIONAL. Contains issued Credential.
     * It MUST be present when transaction_id is not returned.
     * It MAY be a string or an object, depending on the Credential format.
     * See Appendix A for the Credential format specific encoding requirements.
     */
    var credential: JsonNode,
) : CredentialResponse()

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class CredentialResponseAsync (
    /**
     * transaction_id: OPTIONAL. String identifying a Deferred Issuance transaction.
     * This claim is contained in the response if the Credential Issuer was unable to immediately issue the Credential.
     * The value is subsequently used to obtain the respective Credential with the Deferred Credential Endpoint (see Section 9).
     * It MUST be present when the credential parameter is not returned.
     * It MUST be invalidated after the Credential for which it was meant has been obtained by the Wallet.
     */
    var transactionId: String
) : CredentialResponse()



@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
class DeferredCredentialRequest (
    /**
     * transaction_id: REQUIRED. String identifying a Deferred Issuance transaction.
     */
    var transactionId: String
)



/**
 * Credential Format constants.
 * Defined in Appendix A.
 */
enum class CredentialFormatEnum(override val value: String) : JsonValueEnum {
    /**
     * VC signed as a JWT, not using JSON-LD (jwt_vc_json)
     */
    VC_JWT_JSON("jwt_vc_json"),
    /**
     * VC signed as a JWT, using JSON-LD (jwt_vc_json-ld)
    */
    VC_JWT_JSON_LD("jwt_vc_json-ld"),
    /**
     * VC secured using Data Integrity, using JSON-LD, with a proof suite requiring Linked Data canonicalization (ldp_vc)
     */
    VC_LDP_JSON_LD("ldp_vc"),
    /**
     * Credential Format Profile for credentials complying with [ISO.18013-5] (Mobile Driving License).
     */
    MSO_MDOC("mso_mdoc"),
    /**
     * Credential Format Profile for Credentials complying with [I-D.ietf-oauth-sd-jwt-vc] (SD-JWT).
     */
    VC_SD_JWT("vc+sd-jwt"),
}


@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(
    use =  JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "proof_type",
)
@JsonSubTypes(
    Type(JwtProof::class,   name = "jwt"),
    Type(CwtProof::class,   name = "cwt"),
    Type(LdpVpProof::class, name = "ldp_vp"),
)
abstract class Proof (
    @JsonTypeId
    var proofType: ProofTypeEnum,
)

@JsonNaming(SnakeCaseStrategy::class)
class JwtProof(
    var jwt: String,
) : Proof(ProofTypeEnum.JWT)

@JsonNaming(SnakeCaseStrategy::class)
class CwtProof(
    // TODO: encoding of the cbor value is not specified properly, maybe they want base64url, but who knows
    var cwt: String,
) : Proof(ProofTypeEnum.CWT)

@JsonNaming(SnakeCaseStrategy::class)
class LdpVpProof(
    var ldpVp: ObjectNode,
) : Proof(ProofTypeEnum.LDP_VP)

enum class ProofTypeEnum(override val value: String) : JsonValueEnum {
    /**
     * jwt: A JWT [RFC7519] is used as proof of possession.
     * When proof_type is jwt, a proof object MUST include a jwt claim containing a JWT defined in Section 7.2.1.1.
     */
    JWT("jwt"),
    /**
     * cwt: A CWT [RFC8392] is used as proof of possession.
     * When proof_type is cwt, a proof object MUST include a cwt claim containing a CWT defined in Section 7.2.1.3.
     */
    CWT("cwt"),
    /**
     * ldp_vp: A W3C Verifiable Presentation object signed using the Data Integrity Proof as defined in [VC_DATA_20] or [VC_DATA], and where the proof of possession MUST be done in accordance with [VC_Data_Integrity].
     * When proof_type is set to ldp_vp, the proof object MUST include a ldp_vp claim containing a W3C Verifiable Presentation defined in Section 7.2.1.2.
     */
    LDP_VP("ldp_vp");
}


@JsonNaming(SnakeCaseStrategy::class)
data class CredentialResponseEncryption (
    /**
     * <code>jwk</code>: REQUIRED. Object containing a single public key as a JWK used for encrypting the Credential Response.
     */
    var jwk: ObjectNode,
    /**
     * <code>alg</code>: REQUIRED. JWE [RFC7516] alg algorithm [RFC7518] for encrypting Credential Responses.
     */
    var alg: String,
    /**
     * <code>enc</code>: REQUIRED. JWE [RFC7516] enc algorithm [RFC7518] for encrypting Credential Responses.
     */
    var enc: String,
)





@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
class CredentialIssuerMetadata (
    /**
     * credential_issuer: REQUIRED.
     * The Credential Issuer's identifier, as defined in Section 11.2.1.
     */
    var credentialIssuer: String,
    /**
     * authorization_servers: OPTIONAL.
     * Array of strings, where each string is an identifier of the OAuth 2.0 Authorization Server (as defined in [RFC8414]) the Credential Issuer relies on for authorization.
     * If this parameter is omitted, the entity providing the Credential Issuer is also acting as the Authorization Server, i.e., the Credential Issuer's identifier is used to obtain the Authorization Server metadata.
     * The actual OAuth 2.0 Authorization Server metadata is obtained from the oauth-authorization-server well-known location as defined in Section 3 of [RFC8414].
     * When there are multiple entries in the array, the Wallet may be able to determine which Authorization Server to use by querying the metadata;
     * for example, by examining the grant_types_supported values, the Wallet can filter the server to use based on the grant type it plans to use.
     * When the Wallet is using authorization_server parameter in the Credential Offer as a hint to determine which Authorization Server to use out of multiple, the Wallet MUST NOT proceed with the flow if the authorization_server Credential Offer parameter value does not match any of the entries in the authorization_servers array.
     */
    var authorizationServers: List<String> = listOf(),
    /**
     * credential_endpoint: REQUIRED.
     * URL of the Credential Issuer's Credential Endpoint, as defined in Section 7.2.
     * This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     */
    var credentialEndpoint: String,
    /**
     * batch_credential_endpoint: OPTIONAL.
     * URL of the Credential Issuer's Batch Credential Endpoint, as defined in Section 8.
     * This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     * If omitted, the Credential Issuer does not support the Batch Credential Endpoint.
     */
    var batchCredentialEndpoint: String? = null,
    /**
     * deferred_credential_endpoint: OPTIONAL.
     * URL of the Credential Issuer's Deferred Credential Endpoint, as defined in Section 9.
     * This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     * If omitted, the Credential Issuer does not support the Deferred Credential Endpoint.
     */
    var deferredCredentialEndpoint: String? = null,
    /**
     * notification_endpoint: OPTIONAL.
     * URL of the Credential Issuer's Notification Endpoint, as defined in Section 10.
     * This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     * If omitted, the Credential Issuer does not support the Notification Endpoint.
     */
    var notificationEndpoint: String? = null,
    /**
     * credential_response_encryption: OPTIONAL.
     * Object containing information about whether the Credential Issuer supports encryption of the Credential and Batch Credential Response on top of TLS.
     */
    var credentialResponseEncryption: CredentialResponseEncryptionSupport? = null,
    /**
     * credential_identifiers_supported: OPTIONAL.
     * Boolean value specifying whether the Credential Issuer supports returning credential_identifiers parameter in the authorization_details Token Response parameter, with true indicating support.
     * If omitted, the default value is false.
     */
    var credentialIdentifiersSupported: Boolean = false,
    /**
     * signed_metadata: OPTIONAL.
     * String that is a signed JWT.
     * This JWT contains Credential Issuer metadata parameters as claims.
     * The signed metadata MUST be secured using JSON Web Signature (JWS) [RFC7515] and MUST contain an iat (Issued At) claim, an iss (Issuer) claim denoting the party attesting to the claims in the signed metadata, and sub (Subject) claim matching the Credential Issuer identifier.
     * If the Wallet supports signed metadata, metadata values conveyed in the signed JWT MUST take precedence over the corresponding values conveyed using plain JSON elements.
     * If the Credential Issuer wants to enforce use of signed metadata, it omits the respective metadata parameters from the unsigned part of the Credential Issuer metadata.
     * A signed_metadata metadata value MUST NOT appear as a claim in the JWT.
     * The Wallet MUST establish trust in the signer of the metadata, and obtain the keys to validate the signature before processing the metadata.
     * The concrete mechanism how to do that is out of scope of this specification and MAY be defined in the profiles of this specification.
     */
    var signedMetadata: String? = null,
    /**
     * display: OPTIONAL. Array of objects, where each object contains display properties of a Credential Issuer for a certain language. Below is a non-exhaustive list of valid parameters that MAY be included:
     * name: OPTIONAL. String value of a display name for the Credential Issuer.
     * locale: OPTIONAL. String value that identifies the language of this object represented as a language tag taken from values defined in BCP47 [RFC5646]. There MUST be only one object for each language identifier.
     * logo: OPTIONAL. Object with information about the logo of the Credential Issuer. Below is a non-exhaustive list of parameters that MAY be included:
     *   uri: REQUIRED. String value that contains a URI where the Wallet can obtain the logo of the Credential Issuer. The Wallet needs to determine the scheme, since the URI value could use the https: scheme, the data: scheme, etc.
     *   alt_text: OPTIONAL. String value of the alternative text for the logo image.
     */
    var display: List<IssuerDisplay> = listOf(),
    /**
     * credential_configurations_supported: REQUIRED.
     * Object that describes specifics of the Credential that the Credential Issuer supports issuance of.
     * This object contains a list of name/value pairs, where each name is a unique identifier of the supported Credential being described.
     * This identifier is used in the Credential Offer as defined in Section 4.1.1 to communicate to the Wallet which Credential is being offered.
     * The value is an object that contains metadata about a specific Credential.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var credentialConfigurationsSupported: Map<String, CredentialConfigurationSupported>,
)

@JsonNaming(SnakeCaseStrategy::class)
class CredentialResponseEncryptionSupport (
    /**
     * alg_values_supported: REQUIRED.
     * Array containing a list of the JWE [RFC7516] encryption algorithms (alg values) [RFC7518] supported by the Credential and Batch Credential Endpoint to encode the Credential or Batch Credential Response in a JWT [RFC7519].
     */
    var algValuesSupported: List<String>,
    /**
     * enc_values_supported: REQUIRED.
     * Array containing a list of the JWE [RFC7516] encryption algorithms (enc values) [RFC7518] supported by the Credential and Batch Credential Endpoint to encode the Credential or Batch Credential Response in a JWT [RFC7519].
     */
    var encValuesSupported: List<String>,
    /**
     * encryption_required: REQUIRED.
     * Boolean value specifying whether the Credential Issuer requires the additional encryption on top of TLS for the Credential Response.
     * If the value is true, the Credential Issuer requires encryption for every Credential Response and therefore the Wallet MUST provide encryption keys in the Credential Request.
     * If the value is false, the Wallet MAY choose whether it provides encryption keys or not.
     */
    var encryptionRequired: Boolean,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
open class IssuerDisplay (
    /**
     * name: REQUIRED.
     * String value of a display name for the Credential.
     */
    var name: String,
    /**
     * locale: OPTIONAL.
     * String value that identifies the language of this object represented as a language tag taken from values defined in BCP47 [RFC5646].
     * Multiple display objects MAY be included for separate languages.
     * There MUST be only one object for each language identifier.
     */
    var locale: String?,
    /**
     * logo: OPTIONAL.
     * Object with information about the logo of the Credential.
     * The following non-exhaustive set of parameters MAY be included:
     */
    var logo: Logo? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
open class CredentialDisplay (
    /**
     * name: REQUIRED.
     * String value of a display name for the Credential.
     */
    var name: String,
    /**
     * locale: OPTIONAL.
     * String value that identifies the language of this object represented as a language tag taken from values defined in BCP47 [RFC5646].
     * Multiple display objects MAY be included for separate languages.
     * There MUST be only one object for each language identifier.
     */
    var locale: String?,
    /**
     * logo: OPTIONAL.
     * Object with information about the logo of the Credential.
     * The following non-exhaustive set of parameters MAY be included:
     */
    var logo: Logo? = null,
    /**
     * description: OPTIONAL.
     * String value of a description of the Credential.
     */
    var description: String? = null,
    /**
     * background_color: OPTIONAL.
     * String value of a background color of the Credential represented as numerical color values defined in CSS Color Module Level 37 [CSS-Color].
     */
    var backgroundColor: String? = null,
    /**
     * background_image: OPTIONAL.
     * Object with information about the background image of the Credential.
     * At least the following parameter MUST be included:
     */
    var backgroundImage: BackgroundImage? = null,
    /**
     * text_color: OPTIONAL.
     * String value of a text color of the Credential represented as numerical color values defined in CSS Color Module Level 37 [CSS-Color].
     */
    var textColor: String? = null,
)


@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
class Logo (
    /**
     * uri: REQUIRED.
     * String value that contains a URI where the Wallet can obtain the logo of the Credential from the Credential Issuer.
     * The Wallet needs to determine the scheme, since the URI value could use the https: scheme, the data: scheme, etc.
     */
    var uri: String,
    /**
     * alt_text: OPTIONAL.
     * String value of the alternative text for the logo image.
     */
    var altText: String? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
open class BackgroundImage (
    /**
     * uri: REQUIRED.
     * String value that contains a URI where the Wallet can obtain the background image of the Credential from the Credential Issuer.
     * The Wallet needs to determine the scheme, since the URI value could use the https: scheme, the data: scheme, etc.
     */
    var uri: String,
    /**
     * text_color: OPTIONAL.
     * String value of a text color of the Credential represented as numerical color values defined in CSS Color Module Level 37 [CSS-Color].
     */
    var textColor: String? = null,
)



@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeInfo(
    use =  JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "format",
)
@JsonSubTypes(
    Type(W3cJwtJsonCredentialConfigurationSupported::class, name = "jwt_vc_json"),
    Type(W3cLdpJsonLdCredentialConfigurationSupported::class, name = "ldp_vc"),
    Type(W3cJwtJsonLdCredentialConfigurationSupported::class, name = "jwt_vc_json-ld"),
    Type(MDlCredentialConfigurationSupported::class, name = "mso_mdoc"),
    Type(SdJwtCredentialConfigurationSupported::class, name = "vc+sd-jwt"),
)
open class CredentialConfigurationSupported (
    /**
     * format: REQUIRED.
     * A JSON string identifying the format of this Credential, i.e., jwt_vc_json or ldp_vc.
     * Depending on the format value, the object contains further elements defining the type and (optionally) particular claims the Credential MAY contain and information about how to display the Credential.
     * Appendix A contains Credential Format Profiles introduced by this specification.
     */
    var format: String
) {

    /**
     * scope: OPTIONAL.
     * A JSON string identifying the scope value that this Credential Issuer supports for this particular Credential.
     * The value can be the same across multiple credential_configurations_supported objects.
     * The Authorization Server MUST be able to uniquely identify the Credential Issuer based on the scope value.
     * The Wallet can use this value in the Authorization Request as defined in Section 5.1.2.
     * Scope values in this Credential Issuer metadata MAY duplicate those in the scopes_supported parameter of the Authorization Server.
     */
    var scope: String? = null

    /**
     * cryptographic_binding_methods_supported: OPTIONAL.
     * Array of case sensitive strings that identify the representation of the cryptographic key material that the issued Credential is bound to, as defined in Section 7.1.
     * Support for keys in JWK format [RFC7517] is indicated by the value jwk.
     * Support for keys expressed as a COSE Key object [RFC8152] (for example, used in [ISO.18013-5]) is indicated by the value cose_key.
     * When the Cryptographic Binding Method is a DID, valid values are a did: prefix followed by a method-name using a syntax as defined in Section 3.1 of [DID-Core], but without a :and method-specific-id.
     * For example, support for the DID method with a method-name "example" would be represented by did:example.
     */
    var cryptographicBindingMethodsSupported: List<String> = listOf()

    /**
     * credential_signing_alg_values_supported: OPTIONAL.
     * Array of case sensitive strings that identify the algorithms that the Issuer uses to sign the issued Credential.
     * Algorithm names used are determined by the Credential format and are defined in Appendix A.
     */
    var credentialSigningAlgValuesSupported: List<String> = listOf()

    /**
     * proof_types_supported: OPTIONAL.
     * Object that describes specifics of the key proof(s) that the Credential Issuer supports.
     * This object contains a list of name/value pairs, where each name is a unique identifier of the supported proof type(s).
     * Valid values are defined in Section 7.2.1, other values MAY be used.
     * This identifier is also used by the Wallet in the Credential Request as defined in Section 7.2.
     * The value in the name/value pair is an object that contains metadata about the key proof and contains the following parameters defined by this specification:
     */
    var proofTypesSupported: Map<ProofTypeEnum, ProofTypesSupported> = mapOf()

    /**
     * display: OPTIONAL.
     * Array of objects, where each object contains the display properties of the supported Credential for a certain language.
     */
    var display: List<CredentialDisplay> = listOf()
}


@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown=true)
open class ProofTypesSupported (
    /**
     * proof_signing_alg_values_supported: REQUIRED.
     * Array of case sensitive strings that identify the algorithms that the Issuer supports for this proof type.
     * The Wallet uses one of them to sign the proof.
     * Algorithm names used are determined by the key proof type and are defined in Section 7.2.1.
     */
    var proofSigningAlgValuesSupported: List<String>,
)



@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class W3cJwtJsonCredentialConfigurationSupported (
    /**
     * credential_definition: REQUIRED.
     * Object containing the detailed description of the Credential type.
     */
    var credentialDefinition: JsonCredentialDefinition,

    /**
     * order: OPTIONAL.
     * Array of the claim name values that lists them in the order they should be displayed by the Wallet.
     */
    var order: List<String> = listOf()

) : CredentialConfigurationSupported("jwt_vc_json")

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class W3cJwtJsonLdCredentialConfigurationSupported (
    /**
     * credential_definition: REQUIRED.
     * Object containing the detailed description of the Credential type.
     */
    var credentialDefinition: JsonLdCredentialDefinition,

    /**
     * order: OPTIONAL.
     * Array of the claim name values that lists them in the order they should be displayed by the Wallet.
     */
    var order: List<String> = listOf()

) : CredentialConfigurationSupported("jwt_vc_json-ld")

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class W3cLdpJsonLdCredentialConfigurationSupported (
    /**
     * credential_definition: REQUIRED.
     * Object containing the detailed description of the Credential type.
     */
    var credentialDefinition: JsonLdCredentialDefinition,

    /**
     * order: OPTIONAL.
     * Array of the claim name values that lists them in the order they should be displayed by the Wallet.
     */
    var order: List<String> = listOf()

) : CredentialConfigurationSupported("ldp_vc")


@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class JsonCredentialDefinition (
    /**
     * type: REQUIRED.
     * Array designating the types a certain Credential type supports, according to [VC_DATA], Section 4.3.
     */
    var type: List<String>,
    /**
     * credentialSubject: OPTIONAL.
     * Object containing a list of name/value pairs, where each name identifies a claim offered in the Credential.
     * The value can be another such object (nested data structures), or an array of such objects.
     * To express the specifics about the claim, the most deeply nested value MAY be an object that includes the following parameters defined by this specification (other parameters MAY also be used):
     */
    @get:JsonProperty("credentialSubject")
    var credentialSubject: Map<String, JsonCredentialSubject> = mapOf(),
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class JsonCredentialSubject (
    /**
     * mandatory: OPTIONAL.
     * Boolean which, when set to true, indicates that the Credential Issuer will always include this claim in the issued Credential.
     * If set to false, the claim is not included in the issued Credential if the wallet did not request the inclusion of the claim, and/or if the Credential Issuer chose to not include the claim.
     * If the mandatory parameter is omitted, the default value is false.
     */
    var mandatory: Boolean = false,
    /**
     * value_type: OPTIONAL.
     * String value determining the type of value of the claim.
     * Valid values defined by this specification are string, number, and image media types such as image/jpeg as defined in IANA media type registry for images (https://www.iana.org/assignments/media-types/media-types.xhtml#image).
     * Other values MAY also be used.
     */
    var valueType: String? = null,
    /**
     * display: OPTIONAL.
     * Array of objects, where each object contains display properties of a certain claim in the Credential for a certain language.
     * Below is a non-exhaustive list of valid parameters that MAY be included:
     */
    var display: List<ClaimDisplay> = listOf(),
)


@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class MDlCredentialConfigurationSupported (
    /**
     * doctype: REQUIRED.
     * String identifying the Credential type, as defined in [ISO.18013-5].
     */
    var doctype: String,

    /**
     * claims: OPTIONAL.
     * Object containing a list of name/value pairs, where the name is a certain namespace as defined in [ISO.18013-5] (or any profile of it), and the value is an object.
     * This object also contains a list of name/value pairs, where the name is a claim name value that is defined in the respective namespace and is offered in the Credential.
     * The value is an object detailing the specifics of the claim with the following non-exhaustive list of parameters that MAY be included:
     */
    var claims: ClaimTree? = null,

    /**
     * order: OPTIONAL.
     * Array of the claim name values that lists them in the order they should be displayed by the Wallet.
     */
    var order: List<String> = listOf(),

    ) : CredentialConfigurationSupported("mso_mdoc")


@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class SdJwtCredentialConfigurationSupported (
    /**
     * vct: REQUIRED.
     * String designating the type of a Credential, as defined in [I-D.ietf-oauth-sd-jwt-vc].
     */
    var vct: String,

    /**
     * claims: OPTIONAL.
     * Object containing a list of name/value pairs, where the name is a certain namespace as defined in [ISO.18013-5] (or any profile of it), and the value is an object.
     * This object also contains a list of name/value pairs, where the name is a claim name value that is defined in the respective namespace and is offered in the Credential.
     * The value is an object detailing the specifics of the claim with the following non-exhaustive list of parameters that MAY be included:
     */
    var claims: ClaimTree? = null,

    /**
     * order: OPTIONAL.
     * Array of the claim name values that lists them in the order they should be displayed by the Wallet.
     */
    var order: List<String> = listOf(),

    ) : CredentialConfigurationSupported("vc+sd-jwt")




@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class ClaimDisplay (
    /**
     * name: OPTIONAL.
     * String value of a display name for the claim.
     */
    var name: String? = null,
    /**
     * locale: OPTIONAL.
     * String value that identifies language of this object represented as language tag values defined in BCP47 [RFC5646].
     * There MUST be only one object for each language identifier.
     */
    var locale: String? = null,
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class JsonLdCredentialDefinition (
    /**
     * @context: REQUIRED.
     * Array as defined in [VC_DATA], Section 4.1.
     */
    @get:JsonProperty("@context")
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    var context: List<String>,
    /**
     * type: REQUIRED.
     * Array designating the types a certain Credential type supports, according to [VC_DATA], Section 4.3.
     */
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    var type: List<String>,
    /**
     * credentialSubject: OPTIONAL.
     * Object containing a list of name/value pairs, where each name identifies a claim offered in the Credential.
     * The value can be another such object (nested data structures), or an array of such objects.
     * To express the specifics about the claim, the most deeply nested value MAY be an object that includes the following parameters defined by this specification (other parameters MAY also be used):
     */
    @get:JsonProperty("credentialSubject")
    var credentialSubject: Map<String, JsonCredentialSubject> = mapOf(),
)



@JsonSubTypes(
    Type(ClaimTree::class),
    Type(ClaimLeaf::class),
)
sealed interface Claim

@JsonDeserialize(converter = ClaimTreeDeserializer::class)
@JsonSerialize(converter = ClaimTreeSerializer::class)
class ClaimTree (
    var objTree: Map<String, Claim> = mapOf(),
    var arrTree: Map<String, List<Claim>> = mapOf(),
) : Claim

class ClaimTreeDeserializer : StdConverter<ObjectNode, ClaimTree>() {
    override fun convert(rawValue: ObjectNode): ClaimTree {
        val om = jacksonObjectMapper()
        val objTree = mutableMapOf<String, Claim>()
        val arrTree = mutableMapOf<String, List<Claim>>()

        for ((key, value) in rawValue.fields()) {
            if (value.isArray) {
                arrTree[key] = value.map { nodeToClaim(it, om) }
            } else {
                objTree[key] = nodeToClaim(value, om)
            }
        }

        return ClaimTree(objTree, arrTree)
    }

    private fun nodeToClaim(node: JsonNode, om: ObjectMapper): Claim {
        if (node.isEmpty) {
            return ClaimLeaf()
        } else if (containsLeafValues(node)) {
            return om.treeToValue(node, ClaimLeaf::class.java)
        } else {
            return om.treeToValue(node, ClaimTree::class.java)
        }
    }

    private fun containsLeafValues(node: JsonNode): Boolean {
        val leafFields = setOf("mandatory", "value_type", "display")

        val onlyLeafFields = node.fieldNames().asSequence().all { leafFields.contains(it) }
        // check types of leaf fields
        val mandatoryBool = node["mandatory"]?.isBoolean ?: true
        val valueTypeString = node["value_type"]?.isTextual ?: true
        val displayArray = node["display"]?.isArray ?: true

        return onlyLeafFields && mandatoryBool && valueTypeString && displayArray
    }
}

class ClaimTreeSerializer : StdConverter<ClaimTree, ObjectNode>() {
    override fun convert(tree: ClaimTree): ObjectNode {
        val om = jacksonObjectMapper()
        val result: ObjectNode = om.createObjectNode()

        tree.objTree.forEach { (key, claim) ->
            result.putPOJO(key, claim)
        }

        tree.arrTree.forEach { (key, claims) ->
            result.putArray(key).let { array ->
                claims.forEach { claim ->
                    array.addPOJO(claim)
                }
            }
        }

        return result
    }
}

@JsonNaming(SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ClaimLeaf (
    /**
     * mandatory: OPTIONAL.
     * Boolean which when set to true indicates the claim MUST be present in the issued Credential.
     * If the mandatory property is omitted its default should be assumed to be false.
     */
    var mandatory: Boolean = false,
    /**
     * value_type: OPTIONAL.
     * String value determining the type of value of the claim.
     * Valid values defined by this specification are string, number, and image media types such as image/jpeg as defined in IANA media type registry for images (https://www.iana.org/assignments/media-types/media-types.xhtml#image).
     * Other values MAY also be used.
     */
    var valueType: String? = null,
    /**
     * display: OPTIONAL.
     * Array of objects, where each object contains display properties of a certain claim in the Credential for a certain language.
     */
    var display: List<ClaimDisplay> = listOf(),
) : Claim
