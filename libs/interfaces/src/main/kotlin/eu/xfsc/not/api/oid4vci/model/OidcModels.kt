package eu.xfsc.not.api.oid4vci.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * OpenID Connect Discovery 1.0
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery 1.0</a>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class OidcProviderMetadata (

    @get:JsonUnwrapped
    var oauthProviderMetadata: OauthProviderMetadata,

    /**
     * userinfo_endpoint (RECOMMENDED)
     * URL of the OP's UserInfo Endpoint [OpenID.Core]. This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     */
    var userinfoEndpoint: String? = null,
    /**
     * acr_values_supported (OPTIONAL)
     * JSON array containing a list of the Authentication Context Class References that this OP supports.
     */
    var acrValuesSupported: List<String>? = null,
    /**
     * subject_types_supported (REQUIRED)
     * JSON array containing a list of the Subject Identifier types that this OP supports. Valid types include pairwise and public.
     */
    var subjectTypesSupported: List<String>,
    /**
     * id_token_signing_alg_values_supported (REQUIRED)
     * JSON array containing a list of the JWS signing algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT [JWT].
     * The algorithm RS256 MUST be included.
     * The value none MAY be supported but MUST NOT be used unless the Response Type used returns no ID Token from the Authorization Endpoint (such as when using the Authorization Code Flow).
     */
    var idTokenSigningAlgValuesSupported: List<String>,
    /**
     * id_token_encryption_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWE encryption algorithms (alg values) supported by the OP for the ID Token to encode the Claims in a JWT [JWT].
     */
    var idTokenEncryptionAlgValuesSupported: List<String>? = null,
    /**
     * id_token_encryption_enc_values_supported (OPTIONAL)
     * JSON array containing a list of the JWE encryption algorithms (enc values) supported by the OP for the ID Token to encode the Claims in a JWT [JWT].
     */
    var idTokenEncryptionEncValuesSupported: List<String>? = null,
    /**
     * userinfo_signing_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWS [JWS] signing algorithms (alg values) [JWA] supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
     * The value none MAY be included.
     */
    var userinfoSigningAlgValuesSupported: List<String>? = null,
    /**
     * userinfo_encryption_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWE [JWE] encryption algorithms (alg values) [JWA] supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
     */
    var userinfoEncryptionAlgValuesSupported: List<String>? = null,
    /**
     * userinfo_encryption_enc_values_supported (OPTIONAL)
     * JSON array containing a list of the JWE encryption algorithms (enc values) [JWA] supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
     */
    var userinfoEncryptionEncValuesSupported: List<String>? = null,
    /**
     * request_object_signing_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWS signing algorithms (alg values) supported by the OP for Request Objects, which are described in Section 6.1 of OpenID Connect Core 1.0 [OpenID.Core].
     * These algorithms are used both when the Request Object is passed by value (using the request parameter) and when it is passed by reference (using the request_uri parameter).
     * Servers SHOULD support none and RS256.
     */
    var requestObjectSigningAlgValuesSupported: List<String>? = null,
    /**
     * request_object_encryption_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWE encryption algorithms (alg values) supported by the OP for Request Objects.
     * These algorithms are used both when the Request Object is passed by value and when it is passed by reference.
     */
    var requestObjectEncryptionAlgValuesSupported: List<String>? = null,
    /**
     * request_object_encryption_enc_values_supported (OPTIONAL)
     * JSON array containing a list of the JWE encryption algorithms (enc values) supported by the OP for Request Objects.
     * These algorithms are used both when the Request Object is passed by value and when it is passed by reference.
     */
    var requestObjectEncryptionEncValuesSupported: List<String>? = null,
    /**
     * display_values_supported (OPTIONAL)
     * JSON array containing a list of the display parameter values that the OpenID Provider supports.
     * These values are described in Section 3.1.2.1 of OpenID Connect Core 1.0 [OpenID.Core].
     */
    var displayValuesSupported: List<String>? = null,
    /**
     * claim_types_supported (OPTIONAL)
     * JSON array containing a list of the Claim Types that the OpenID Provider supports.
     * These Claim Types are described in Section 5.6 of OpenID Connect Core 1.0 [OpenID.Core].
     * Values defined by this specification are normal, aggregated, and distributed.
     * If omitted, the implementation supports only normal Claims.
     */
    var claimTypesSupported: List<String>? = null,
    /**
     * claims_supported (RECOMMENDED)
     * JSON array containing a list of the Claim Names of the Claims that the OpenID Provider MAY be able to supply values for.
     * Note that for privacy or other reasons, this might not be an exhaustive list.
     */
    var claimsSupported: List<String>? = null,
    /**
     * claims_locales_supported (OPTIONAL)
     * Languages and scripts supported for values in Claims being returned, represented as a JSON array of BCP47 [RFC5646] language tag values.
     * Not all languages and scripts are necessarily supported for all Claim values.
     */
    var claimsLocalesSupported: List<String>? = null,
    /**
     * claims_parameter_supported (OPTIONAL)
     * Boolean value specifying whether the OP supports use of the claims parameter, with true indicating support.
     * If omitted, the default value is false.
     */
    var claimsParameterSupported: Boolean = false,
    /**
     * request_parameter_supported (OPTIONAL)
     * Boolean value specifying whether the OP supports use of the request parameter, with true indicating support.
     * If omitted, the default value is false.
     */
    var requestParameterSupported: Boolean = false,
    /**
     * request_uri_parameter_supported (OPTIONAL)
     * Boolean value specifying whether the OP supports use of the request_uri parameter, with true indicating support.
     * If omitted, the default value is true.
     */
    var requestUriParameterSupported: Boolean = true,
    /**
     * require_request_uri_registration (OPTIONAL)
     * Boolean value specifying whether the OP requires any request_uri values used to be pre-registered using the request_uris registration parameter.
     * Pre-registration is REQUIRED when the value is true. If omitted, the default value is false.
     */
    var requireRequestUriRegistration: Boolean = false,
)
