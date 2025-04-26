package eu.xfsc.not.oid4vci

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.xfsc.not.api.oid4vci.OauthWellKnownConfigApi
import eu.xfsc.not.api.oid4vci.Oid4VciWellKnownConfigApi
import eu.xfsc.not.api.oid4vci.Oidc4VciApi
import eu.xfsc.not.api.oid4vci.OidcWellKnownConfigApi
import eu.xfsc.not.api.oid4vci.model.*
import eu.xfsc.not.api.oid4vci.model.GrantTypes.PreAuthCodeName
import io.quarkus.security.Authenticated
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jboss.resteasy.reactive.NoCache


@Path("")
class WellKnownConfigApiImpl : OauthWellKnownConfigApi, OidcWellKnownConfigApi, Oid4VciWellKnownConfigApi {

    @Inject lateinit var mdProv: OidcConfigProvider

    override fun oauthConfig(): OauthProviderMetadata {
        return mdProv.buildOauthConfig()
    }

    override fun oidcConfig(): OidcProviderMetadata {
        return mdProv.buildOidcConfig()
    }

    override fun oidVciConfig(): CredentialIssuerMetadata {
        return mdProv.buildVciConfig()
    }

    @Path("oauth/jwks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun oauthJwkSet(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
    }

}


@Path("")
class Oidc4VciImpl : Oidc4VciApi {

    @Inject lateinit var preAuthTokenRequestService: PreAuthTokenRequestService
    @Inject lateinit var credentialService: CredentialIssueService

    @Inject lateinit var accessToken: JsonWebToken

    @NoCache
    override fun oidToken(tokenReq: Oid4VciTokenRequest): Oid4VciTokenResponse {
        return when (tokenReq.grantType) {
            PreAuthCodeName -> {
                preAuthTokenRequestService.processTokenRequest(tokenReq)
            }
            else -> {
                OauthErrorResponse(OauthErrorResponse.UnsupportedGrantType).throwError()
            }
        }
    }

    @NoCache
    @Authenticated
    override fun oidCredential(credentialReq: CredentialRequest): Response {
        var respObj = credentialService.processCredentialRequest(accessToken, credentialReq)
        return credentialService.encryptResponse(respObj, credentialReq.credentialResponseEncryption)
    }

//    @NoCache
//    @Authenticated
//    override fun oidDeferredCredential(deferredCredentialReq: DeferredCredentialRequest): CredentialResponseSync {
//        TODO("Not yet implemented")
//    }
}
