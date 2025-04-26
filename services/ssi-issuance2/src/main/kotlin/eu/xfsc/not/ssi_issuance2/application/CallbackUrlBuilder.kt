package eu.xfsc.not.ssi_issuance2.application

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI

@ApplicationScoped
class CallbackUrlBuilder {

    @ConfigProperty(name = "issuance2.service.url")
    lateinit var url: URI

    private fun buildUrl(path: String, token: String): URI {
        return UriBuilder.fromUri(url).let {
            it.path(path)
            it.path(token)
            it.build()
        }
    }

    fun buildCancelUrl(token: String) = buildUrl(
        "/api/v2/issuance/session",
        token,
    )

    fun buildOidIssuanceApiUrl(token: String) = buildUrl(
        "/api/v2/oid-issuance/",
        token,
    )

}
