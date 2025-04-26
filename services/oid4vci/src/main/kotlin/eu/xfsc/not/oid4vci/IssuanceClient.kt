package eu.xfsc.not.oid4vci

import eu.gaiax.notarization.api.issuance.IssueCredentialFailure
import eu.gaiax.notarization.api.issuance.OidIssuanceApi
import eu.gaiax.notarization.api.issuance.OidIssuanceApiRaw
import eu.gaiax.notarization.api.issuance.VerifyProofFailure
import io.quarkus.rest.client.reactive.ClientExceptionMapper
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import java.lang.reflect.Method
import java.net.URI
import kotlin.reflect.jvm.javaMethod


@ApplicationScoped
class IssuanceClientBuilder {
    fun buildClient(callbackUrl: String): OidIssuanceApiRaw {
        val client = QuarkusRestClientBuilder.newBuilder()
            .baseUri(URI.create(callbackUrl))
            .build(OidIssuanceApiRaw::class.java)
        return client
    }
}

interface OidIssuanceApiClient : OidIssuanceApi {
    companion object {
        @ClientExceptionMapper
        fun toException(response: Response, method: Method): Exception? {
            return when (response.status) {
                400 -> when (method) {
                    OidIssuanceApiClient::verifyProof.javaMethod -> VerifyProofException(response.readEntity(VerifyProofFailure::class.java))
                    OidIssuanceApiClient::issueCredential.javaMethod -> IssueCredentialException(response.readEntity(IssueCredentialFailure::class.java))
                    else -> null
                }
                else -> null
            }
        }
    }
}

class VerifyProofException(val resp: VerifyProofFailure) : Exception()
class IssueCredentialException(val resp: IssueCredentialFailure) : Exception()
