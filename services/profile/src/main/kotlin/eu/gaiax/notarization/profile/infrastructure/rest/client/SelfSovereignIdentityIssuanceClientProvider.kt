package eu.gaiax.notarization.profile.infrastructure.rest.client

import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.profile.infrastructure.config.SelfSovereignIdentityIssuanceConfig
import io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilder
import io.quarkus.runtime.StartupEvent
import io.smallrye.config.Priorities
import jakarta.annotation.Priority
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URL
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {}

class SelfSovereignIdentityIssuanceClientProvider {

    companion object {
        const val clientV1Key = "ssi-issuance-v1-api"
        const val clientV2Key = "ssi-issuance-v2-api"
    }

    @Inject
    lateinit var config: SelfSovereignIdentityIssuanceConfig

    @ConfigProperty(name = "quarkus.rest-client.$clientV1Key.url")
    lateinit var issuanceV1ClientUrl: Optional<URL>

    @ConfigProperty(name = "quarkus.rest-client.$clientV2Key.url")
    lateinit var issuanceV2ClientUrl: Optional<URL>

    @Produces
    fun produceSupportedVersion(): Set<ApiVersion> {

        val results = mutableSetOf<ApiVersion>()
        if (issuanceV1ClientUrl.isPresent) {
            results.add(ApiVersion.V1)
        }
        if (issuanceV2ClientUrl.isPresent) {
            results.add(ApiVersion.V2)
        }
        if (results.isEmpty()) {
            logger.error { "Both versions (v1 and v2) of the SSI-Issuance HTTP clients are disabled. Until at least one of these is activated, profiles cannot properly be initiated." }
        } else {
            logger.info { "Running profile service with support for issuance services $results" }
        }
        return results
    }

    @Produces
    fun provideV1Client(): Optional<SsiIssuanceV1HttpClient> {

        return if (issuanceV1ClientUrl.isEmpty) {
            Optional.empty()
        } else {
            Optional.of(
                RestClientCDIDelegateBuilder.createDelegate(SsiIssuanceV1HttpClient::class.java, null, clientV1Key)
            )
        }
    }

    @Produces
    fun provideV2Client(): Optional<SsiIssuanceV2HttpClient> {

        return if (issuanceV2ClientUrl.isEmpty) {
            Optional.empty()
        } else {
            Optional.of(
                RestClientCDIDelegateBuilder.createDelegate(SsiIssuanceV2HttpClient::class.java, null, clientV2Key)
            )
        }
    }

}
