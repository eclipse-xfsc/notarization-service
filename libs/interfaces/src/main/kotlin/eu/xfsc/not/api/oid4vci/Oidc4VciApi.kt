package eu.xfsc.not.api.oid4vci

import eu.xfsc.not.api.oid4vci.model.*
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation


interface OauthWellKnownConfigApi {
    @Operation(
        summary = "OAuth Configuration",
        description = "Retrieves the OAuth Configuration.",
    )

    @Path(".well-known/oauth-authorization-server")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun oauthConfig() : OauthProviderMetadata
}

interface OidcWellKnownConfigApi {
    @Operation(
        summary = "OIDC Configuration",
        description = "Retrieves the OIDC Configuration.",
    )

    @Path(".well-known/openid-configuration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun oidcConfig() : OidcProviderMetadata
}

interface Oid4VciWellKnownConfigApi {

    @Operation(
        summary = "OID4VCI Credential Issuer Metadata",
        description = "Retrieves the OID4VCI Credential Issuer Metadata (cf. Sec. 10.2).",
    )

    @Path(".well-known/openid-credential-issuer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun oidVciConfig() : CredentialIssuerMetadata
}


interface Oidc4VciApi {
    @Operation(
        summary = "OID4VCI Token Endpoint",
        description = "Retrieves the token for the following credential issuance (cf. Sec. 6).",
    )

    @Path("/oid4vci/token")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun oidToken(@BeanParam tokenReq: Oid4VciTokenRequest) : Oid4VciTokenResponse


    @Operation(
        summary = "OID4VCI Credential Endpoint",
        description = "Requests credential retrieval (cf. Sec. 7).",
    )

    @Path("/oid4vci/credential")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON, "application/jwt")
    // Response is CredentialResponse or JWT
    fun oidCredential(credentialReq: CredentialRequest) : Response


//    @Operation(
//        summary = "OID4VCI Deferred Credential Endpoint",
//        description = "Retrieves the issued credential (cf. Sec. 9).",
//    )
//
//    @Path("/oid4vci/deferred-credential")
//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    fun oidDeferredCredential(deferredCredentialReq: DeferredCredentialRequest) : CredentialResponseSync
}
