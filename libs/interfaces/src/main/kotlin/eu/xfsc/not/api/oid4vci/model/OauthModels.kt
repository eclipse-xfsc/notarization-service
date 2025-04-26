package eu.xfsc.not.api.oid4vci.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * Authorization servers can have metadata describing their configuration.
 * The following authorization server metadata values are used by this specification and are registered in the IANA "OAuth Authorization Server Metadata" registry established in Section 7.1:
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
open class OauthProviderMetadata (
    /**
     * issuer (REQUIRED)
     * URL using the https scheme with no query or fragment components that the OP asserts as its Issuer Identifier. If Issuer discovery is supported (see Section 2), this value MUST be identical to the issuer value returned by WebFinger. This also MUST be identical to the iss Claim value in ID Tokens issued from this Issuer.
     */
    var issuer: String,
    /**
     * authorization_endpoint (REQUIRED)
     * URL of the OP's OAuth 2.0 Authorization Endpoint [OpenID.Core]. This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     */
    var authorizationEndpoint: String? = null,
    /**
     * token_endpoint
     * URL of the OP's OAuth 2.0 Token Endpoint [OpenID.Core]. This is REQUIRED unless only the Implicit Flow is used. This URL MUST use the https scheme and MAY contain port, path, and query parameter components.
     */
    var tokenEndpoint: String,
    /**
     * jwks_uri (REQUIRED)
     * URL of the OP's JWK Set [JWK] document, which MUST use the https scheme.
     * This contains the signing key(s) the RP uses to validate signatures from the OP.
     * The JWK Set MAY also contain the Server's encryption key(s), which are used by RPs to encrypt requests to the Server.
     * When both signing and encryption keys are made available, a use (public key use) parameter value is REQUIRED for all keys in the referenced JWK Set to indicate each key's intended usage.
     * Although some algorithms allow the same key to be used for both signatures and encryption, doing so is NOT RECOMMENDED, as it is less secure.
     * The JWK x5c parameter MAY be used to provide X.509 representations of keys provided.
     * When used, the bare key values MUST still be present and MUST match those in the certificate.
     * The JWK Set MUST NOT contain private or symmetric key values.
     */
    var jwksUri: String,
    /**
     * registration_endpoint (RECOMMENDED)
     * URL of the OP's Dynamic Client Registration Endpoint [OpenID.Registration], which MUST use the https scheme.
     */
    var registrationEndpoint: String? = null,
    /**
     * scopes_supported (RECOMMENDED)
     * JSON array containing a list of the OAuth 2.0 [RFC6749] scope values that this server supports.
     * The server MUST support the openid scope value.
     * Servers MAY choose not to advertise some supported scope values even when this parameter is used, although those defined in [OpenID.Core] SHOULD be listed, if supported.
     */
    var scopesSupported: List<String>? = null,
    /**
     * response_types_supported (REQUIRED)
     * JSON array containing a list of the OAuth 2.0 response_type values that this OP supports.
     * Dynamic OpenID Providers MUST support the code, id_token, and the id_token token Response Type values.
     */
    var responseTypesSupported: List<String>,
    /**
     * response_modes_supported (OPTIONAL)
     * JSON array containing a list of the OAuth 2.0 response_mode values that this OP supports, as specified in OAuth 2.0 Multiple Response Type Encoding Practices [OAuth.Responses].
     * If omitted, the default for Dynamic OpenID Providers is ["query", "fragment"].
     */
    var responseModesSupported: List<String>? = null,
    /**
     * grant_types_supported (OPTIONAL)
     * JSON array containing a list of the OAuth 2.0 Grant Type values that this OP supports.
     * Dynamic OpenID Providers MUST support the authorization_code and implicit Grant Type values and MAY support other Grant Types.
     * If omitted, the default value is ["authorization_code", "implicit"].
     */
    var grantTypesSupported: List<String>? = null,
    /**
     * token_endpoint_auth_methods_supported (OPTIONAL)
     * JSON array containing a list of Client Authentication methods supported by this Token Endpoint.
     * The options are client_secret_post, client_secret_basic, client_secret_jwt, and private_key_jwt, as described in Section 9 of OpenID Connect Core 1.0 [OpenID.Core].
     * Other authentication methods MAY be defined by extensions.
     * If omitted, the default is client_secret_basic -- the HTTP Basic Authentication Scheme specified in Section 2.3.1 of OAuth 2.0 [RFC6749].
     */
    var tokenEndpointAuthMethodsSupported: List<String>? = null,
    /**
     * token_endpoint_auth_signing_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWS signing algorithms (alg values) supported by the Token Endpoint for the signature on the JWT [JWT] used to authenticate the Client at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods.
     * Servers SHOULD support RS256. The value none MUST NOT be used.
     */
    var tokenEndpointAuthSigningAlgValuesSupported: List<String>? = null,
    /**
     * service_documentation (OPTIONAL)
     * URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider.
     * In particular, if the OpenID Provider does not support Dynamic Client Registration, then information on how to register Clients needs to be provided in this documentation.
     */
    var serviceDocumentation: String? = null,
    /**
     * ui_locales_supported (OPTIONAL)
     * Languages and scripts supported for the user interface, represented as a JSON array of BCP47 [RFC5646] language tag values.
     */
    var uiLocalesSupported: List<String>? = null,
    /**
     * op_policy_uri (OPTIONAL)
     * URL that the OpenID Provider provides to the person registering the Client to read about the OP's requirements on how the Relying Party can use the data provided by the OP.
     * The registration process SHOULD display this URL to the person registering the Client if it is given.
     */
    var opPolicyUri: String? = null,
    /**
     * op_tos_uri (OPTIONAL)
     * URL that the OpenID Provider provides to the person registering the Client to read about the OpenID Provider's terms of service.
     * The registration process SHOULD display this URL to the person registering the Client if it is given.
     */
    var opTosUri: String? = null,
    /**
     * revocation_endpoint (OPTIONAL)
     * URL of the authorization server's OAuth 2.0 revocation endpoint [RFC7009].
     */
    var revocationEndpoint: String? = null,
    /**
     * revocation_endpoint_auth_methods_supported (OPTIONAL)
     * JSON array containing a list of client authentication methods supported by this revocation endpoint.
     * The valid client authentication method values are those registered in the IANA "OAuth Token Endpoint Authentication Methods" registry [IANA.OAuth.Parameters].
     * If omitted, the default is "client_secret_basic" -- the HTTP Basic Authentication Scheme specified in Section 2.3.1 of OAuth 2.0 [RFC6749].
     */
    var revocationEndpointAuthMethodsSupported: List<String>? = null,
    /**
     * revocation_endpoint_auth_signing_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWS signing algorithms ("alg" values) supported by the revocation endpoint for the signature on the JWT [JWT] used to authenticate the client at the revocation endpoint for the "private_key_jwt" and "client_secret_jwt" authentication methods.
     * This metadata entry MUST be present if either of these authentication methods are specified in the "revocation_endpoint_auth_methods_supported" entry.
     * No default algorithms are implied if this entry is omitted.
     * The value "none" MUST NOT be used.
     */
    var revocationEndpointAuthSigningAlgValuesSupported: List<String>? = null,
    /**
     * introspection_endpoint (OPTIONAL)
     * URL of the authorization server's OAuth 2.0 introspection endpoint [RFC7662].
     */
    var introspectionEndpoint: String? = null,
    /**
     * introspection_endpoint_auth_methods_supported (OPTIONAL)
     * JSON array containing a list of client authentication methods supported by this introspection endpoint.
     * The valid client authentication method values are those registered in the IANA "OAuth Token Endpoint Authentication Methods" registry [IANA.OAuth.Parameters] or those registered in the IANA "OAuth Access Token Types" registry [IANA.OAuth.Parameters].
     * (These values are and will remain distinct, due to Section 7.2.)
     * If omitted, the set of supported authentication methods MUST be determined by other means.
     */
    var introspectionEndpointAuthMethodsSupported: List<String>? = null,
    /**
     * introspection_endpoint_auth_signing_alg_values_supported (OPTIONAL)
     * JSON array containing a list of the JWS signing algorithms ("alg" values) supported by the introspection endpoint for the signature on the JWT [JWT] used to authenticate the client at the introspection endpoint for the "private_key_jwt" and "client_secret_jwt" authentication methods.
     * This metadata entry MUST be present if either of these authentication methods are specified in the "introspection_endpoint_auth_methods_supported" entry.
     * No default algorithms are implied if this entry is omitted.
     * The value "none" MUST NOT be used.
     */
    var introspectionEndpointAuthSigningAlgValuesSupported: List<String>? = null,
    /**
     * code_challenge_methods_supported (OPTIONAL)
     * JSON array containing a list of Proof Key for Code Exchange (PKCE) [RFC7636] code challenge methods supported by this authorization server.
     * Code challenge method values are used in the "code_challenge_method" parameter defined in Section 4.3 of [RFC7636].
     * The valid code challenge method values are those registered in the IANA "PKCE Code Challenge Methods" registry [IANA.OAuth.Parameters].
     * If omitted, the authorization server does not support PKCE.
     */
    var codeChallengeMethodsSupported: List<String>? = null,

    // OID4VCI
    /**
     * pre-authorized_grant_anonymous_access_supported (OPTIONAL)
     * A boolean indicating whether the Credential Issuer accepts a Token Request with a Pre-Authorized Code but without a client_id.
     * The default is false.
     */
    @JsonProperty(value = "pre-authorized_grant_anonymous_access_supported")
    var preAuthorizedGrantAnonymousAccessSupported: Boolean = false,
)


object TokenTypes {
    const val Bearer = "Bearer"
}

object OAuthResponseModes {
    const val QUERY = "query"
    const val FRAGMENT = "fragment"
    const val FORM_POST = "form_post"
    /**
     * In this mode, the Authorization Response is sent to the Verifier using an HTTPS POST request to an endpoint controlled by the Verifier.
     * The Authorization Response parameters are encoded in the body using the application/x-www-form-urlencoded content type.
     * The flow can end with an HTTPS POST request from the Wallet to the Verifier, or it can end with a redirect that follows the HTTPS POST request, if the Verifier responds with a redirect URI to the Wallet.
     */
    const val DIRECT_POST = "direct_post"
}


open class OauthTokenRequest {
    @HeaderParam("Authorization")
    var authorization: String? = null

    /**
     * grant_type REQUIRED.
     * Value MUST be set to "authorization_code".
     */
    @FormParam("grant_type")
    @NotBlank
    var grantType: String = GrantTypes.CodeName

    /**
     * code REQUIRED.
     * The authorization code received from the authorization server.
     */
    @FormParam("code")
    var code: String? = null

    /**
     * redirect_uri REQUIRED,
     * if the "redirect_uri" parameter was included in the authorization request as described in Section 4.1.1, and their values MUST be identical.
     */
    @FormParam("redirect_uri")
    var redirectUri: String? = null

    /**
     * client_id REQUIRED,
     * if the client is not authenticating with the authorization server as described in Section 3.2.1.
     */
    @FormParam("client_id")
    var clientId: String? = null
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class OauthTokenResponse (
    /**
     * access_token REQUIRED.
     * The access token issued by the authorization server.
     */
    var accessToken: String,
    /**
     * token_type REQUIRED.
     * The type of the token issued as described in Section 7.1.
     * Value is case insensitive.
     */
    var tokenType: String,
    /**
     * expires_in RECOMMENDED.
     * The lifetime in seconds of the access token.
     * For example, the value "3600" denotes that the access token will expire in one hour from the time the response was generated.
     * If omitted, the authorization server SHOULD provide the expiration time via other means or document the default value.
     */
    var expiresIn: Long? = null,
    /**
     * refresh_token OPTIONAL.
     * The refresh token, which can be used to obtain new access tokens using the same authorization grant as described in Section 6.
     */
    var refreshToken: String? = null,
    /**
     * scope OPTIONAL,
     * if identical to the scope requested by the client; otherwise, REQUIRED.
     * The scope of the access token as described by Section 3.3.
     */
    var scope: String? = null,
)

class OauthException(
    val errorResponse: OauthErrorResponse
) : WebApplicationException(
    Response.status(400)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(errorResponse)
        .build()
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class OauthErrorResponse (
    /**
     * error REQUIRED.
     * A single ASCII [USASCII] error code.
     */
    val error: String,
    /**
     * error_description OPTIONAL.
     * Human-readable ASCII [USASCII] text providing additional information, used to assist the client developer in understanding the error that occurred.
     * Values for the "error_description" parameter MUST NOT include characters outside the set %x20-21 / %x23-5B / %x5D-7E.
     */
    var errorDescription: String? = null,
    /**
     * error_uri OPTIONAL.
     * A URI identifying a human-readable web page with information about the error, used to provide the client developer with additional information about the error.
     * Values for the "error_uri" parameter MUST conform to the URI-reference syntax and thus MUST NOT include characters outside the set %x21 / %x23-5B / %x5D-7E.
     */
    var errorUri: String? = null,
) {
    fun throwError(): Nothing {
        throw OauthException(this)
    }

    companion object {
        /**
         * invalid_request
         * The request is missing a required parameter, includes an unsupported parameter value (other than grant type), repeats a parameter, includes multiple credentials, utilizes more than one mechanism for authenticating the client, or is otherwise malformed.
         *
         * The Authorization Server does not expect a Transaction Code in the Pre-Authorized Code Flow but the Client provides a Transaction Code.
         * The Authorization Server expects a Transaction Code in the Pre-Authorized Code Flow but the Client does not provide a Transaction Code.
         */
        const val InvalidRequest = "invalid_request"
        /**
         * invalid_client
         * Client authentication failed (e.g., unknown client, no client authentication included, or unsupported authentication method).
         * The authorization server MAY return an HTTP 401 (Unauthorized) status code to indicate which HTTP authentication schemes are supported.
         * If the client attempted to authenticate via the "Authorization" request header field, the authorization server MUST respond with an HTTP 401 (Unauthorized) status code and include the "WWW-Authenticate" response header field matching the authentication scheme used by the client.
         *
         * The Client tried to send a Token Request with a Pre-Authorized Code without a Client ID but the Authorization Server does not support anonymous access.
         */
        const val InvalidClient = "invalid_client"
        /**
         * invalid_grant
         * The provided authorization grant (e.g., authorization code, resource owner credentials) or refresh token is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client.
         *
         * The Authorization Server expects a Transaction Code in the Pre-Authorized Code Flow but the Client provides the wrong Transaction Code.
         * The End-User provides the wrong Pre-Authorized Code or the Pre-Authorized Code has expired.
         */
        const val InvalidGrant = "invalid_grant"
        /**
         * unauthorized_client
         * The authenticated client is not authorized to use this authorization grant type.
         */
        const val UnauthorizedClient = "unauthorized_client"
        /**
         * unsupported_grant_type
         * The authorization grant type is not supported by the authorization server.
         */
        const val UnsupportedGrantType = "unsupported_grant_type"
        /**
         * invalid_scope
         * The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner.
         */
        const val InvalidScope = "invalid_scope"
    }
}


/**
 * OAuth Client Metadata
 * @see [OAuth 2.0 Dynamic Registration, Sec. 2](https://www.rfc-editor.org/rfc/rfc7591.html#section-2)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class OauthClientMetadata (
    /**
     * redirect_uris
     * Array of redirection URI strings for use in redirect-based flows such as the authorization code and implicit flows.
     * As required by Section 2 of OAuth 2.0 [RFC6749](https://www.rfc-editor.org/rfc/rfc6749), clients using flows with redirection MUST register their redirection URI values.
     * Authorization servers that support dynamic registration for redirect-based flows MUST implement support for this metadata value.
     */
    var redirectUris: List<String> = listOf(),
    /**
     * token_endpoint_auth_method
     * String indicator of the requested authentication method for the token endpoint.
     * Values defined by this specification are:
     * *  "none": The client is a public client as defined in OAuth 2.0, Section 2.1, and does not have a client secret.
     * *  "client_secret_post": The client uses the HTTP POST parameters as defined in OAuth 2.0, Section 2.3.1.
     * *  "client_secret_basic": The client uses HTTP Basic as defined in OAuth 2.0, Section 2.3.1.
     *
     * Additional values can be defined via the IANA "OAuth Token Endpoint Authentication Methods" registry established in Section 4.2.
     * Absolute URIs can also be used as values for this parameter without being registered.
     * If unspecified or omitted, the default is "client_secret_basic", denoting the HTTP Basic authentication scheme as specified in Section 2.3.1 of OAuth 2.0.
     */
    var tokenEndpointAuthMethod: String? = null,
    /**
     * grant_types
     * Array of OAuth 2.0 grant type strings that the client can use at the token endpoint.
     * These grant types are defined as follows:
     * *  "authorization_code": The authorization code grant type defined in OAuth 2.0, Section 4.1.
     * *  "implicit": The implicit grant type defined in OAuth 2.0, Section 4.2.
     * *  "password": The resource owner password credentials grant type defined in OAuth 2.0, Section 4.3.
     * *  "client_credentials": The client credentials grant type defined in OAuth 2.0, Section 4.4.
     * *  "refresh_token": The refresh token grant type defined in OAuth 2.0, Section 6.
     * *  "urn:ietf:params:oauth:grant-type:jwt-bearer": The JWT Bearer Token Grant Type defined in OAuth JWT Bearer Token Profiles [RFC7523].
     * *  "urn:ietf:params:oauth:grant-type:saml2-bearer": The SAML 2.0 Bearer Assertion Grant defined in OAuth SAML 2 Bearer Token Profiles [RFC7522].
     *
     * If the token endpoint is used in the grant type, the value of this parameter MUST be the same as the value of the "grant_type" parameter passed to the token endpoint defined in the grant type definition.
     * Authorization servers MAY allow for other values as defined in the grant type extension process described in OAuth 2.0, Section 4.5.
     * If omitted, the default behavior is that the client will use only the "authorization_code" Grant Type.
     */
    var grantTypes: List<String> = listOf(),
    /**
     * response_types
     * Array of the OAuth 2.0 response type strings that the client can use at the authorization endpoint.
     * These response types are defined as follows:
     * *  "code": The authorization code response type defined in OAuth 2.0, Section 4.1.
     * *  "token": The implicit response type defined in OAuth 2.0, Section 4.2.
     *
     * If the authorization endpoint is used by the grant type, the value of this parameter MUST be the same as the value of the "response_type" parameter passed to the authorization endpoint defined in the grant type definition.
     * Authorization servers MAY allow for other values as defined in the grant type extension process is described in OAuth 2.0, Section 4.5.
     * If omitted, the default is that the client will use only the "code" response type.
     */
    var responseTypes: List<String> = listOf(),
    /**
     * client_name
     * Human-readable string name of the client to be presented to the end-user during authorization.
     * If omitted, the authorization server MAY display the raw "client_id" value to the end-user instead.
     * It is RECOMMENDED that clients always send this field.
     * The value of this field MAY be internationalized, as described in Section 2.2.
     */
    var clientName: String? = null,
    /**
     * client_uri
     * URL string of a web page providing information about the client.
     * If present, the server SHOULD display this URL to the end-user in a clickable fashion.
     * It is RECOMMENDED that clients always send this field.
     * The value of this field MUST point to a valid web page.
     * The value of this field MAY be internationalized, as described in Section 2.2.
     */
    var clientUri: String? = null,
    /**
     * logo_uri
     * URL string that references a logo for the client.
     * If present, the server SHOULD display this image to the end-user during approval.
     * The value of this field MUST point to a valid image file.
     * The value of this field MAY be internationalized, as described in Section 2.2.
     */
    var logoUri: String? = null,
    /**
     * scope
     * String containing a space-separated list of scope values (as described in Section 3.3 of OAuth 2.0 [RFC6749](https://www.rfc-editor.org/rfc/rfc6749)) that the client can use when requesting access tokens.
     * The semantics of values in this list are service specific.
     * If omitted, an authorization server MAY register a client with a default set of scopes.
     */
    val scope: String? = null,
    /**
     * contacts
     * Array of strings representing ways to contact people responsible for this client, typically email addresses.
     * The authorization server MAY make these contact addresses available to end-users for support requests for the client.
     * See Section 6 for information on Privacy Considerations.
     */
    val contacts: List<String> = listOf(),
    /**
     * tos_uri
     * URL string that points to a human-readable terms of service document for the client that describes a contractual relationship between the end-user and the client that the end-user accepts when authorizing the client.
     * The authorization server SHOULD display this URL to the end-user if it is provided.
     * The value of this field MUST point to a valid web page.
     * The value of this field MAY be internationalized, as described in Section 2.2.
     */
    val tosUri: String? = null,
    /**
     * policy_uri
     * URL string that points to a human-readable privacy policy document that describes how the deployment organization collects, uses, retains, and discloses personal data.
     * The authorization server SHOULD display this URL to the end-user if it is provided.
     * The value of this field MUST point to a valid web page.
     * The value of this field MAY be internationalized, as described in Section 2.2.
     */
    val policyUri: String? = null,
    /**
     * jwks_uri
     * URL string referencing the client's JSON Web Key (JWK) Set [RFC7517] document, which contains the client's public keys.
     * The value of this field MUST point to a valid JWK Set document.
     * These keys can be used by higher-level protocols that use signing or encryption.
     * For instance, these keys might be used by some applications for validating signed requests made to the token endpoint when using JWTs for client authentication [RFC7523].
     * Use of this parameter is preferred over the "jwks" parameter, as it allows for easier key rotation.
     * The "jwks_uri" and "jwks" parameters MUST NOT both be present in the same request or response.
     */
    val jwksUri: String? = null,
    /**
     * jwks
     * Client's JSON Web Key Set [RFC7517] document value, which contains the client's public keys.
     * The value of this field MUST be a JSON object containing a valid JWK Set.
     * These keys can be used by higher-level protocols that use signing or encryption.
     * This parameter is intended to be used by clients that cannot use the "jwks_uri" parameter, such as native clients that cannot host public URLs.
     * The "jwks_uri" and "jwks" parameters MUST NOT both be present in the same request or response.
     */
    val jwks: ObjectNode? = null,
    /**
     * software_id
     *  A unique identifier string (e.g., a Universally Unique Identifier (UUID)) assigned by the client developer or software publisher used by registration endpoints to identify the client software to be dynamically registered.
     *  Unlike "client_id", which is issued by the authorization server and SHOULD vary between instances, the "software_id" SHOULD remain the same for all instances of the client software.
     *  The "software_id" SHOULD remain the same across multiple updates or versions of the same piece of software.
     *  The value of this field is not intended to be human readable and is usually opaque to the client and authorization server.
     */
    val softwareId: String? = null,
    /**
     * software_version
     * A version identifier string for the client software identified by "software_id".
     * The value of the "software_version" SHOULD change on any update to the client software identified by the same "software_id".
     * The value of this field is intended to be compared using string equality matching and no other comparison semantics are defined by this specification.
     * The value of this field is outside the scope of this specification, but it is not intended to be human readable and is usually opaque to the client and authorization server.
     * The definition of what constitutes an update to client software that would trigger a change to this value is specific to the software itself and is outside the scope of this specification.
     */
    val softwareVersion: String? = null,
)
