package eu.xfsc.not.ssi_issuance2

import eu.xfsc.not.vc.DidHandler
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*


@ApplicationScoped
class DidHandlerInit {
    @ConfigProperty(name="gaia-x.did.useUniResolver")
    var useUniResolver: Boolean = false
    @ConfigProperty(name="gaia-x.did.resolver.url")
    lateinit var didResolverUrl: Optional<String>
    @ConfigProperty(name="gaia-x.did.registrar.url")
    lateinit var didRegistrarUrl: Optional<String>

    fun onStart(@Observes ev: StartupEvent?) {
        DidHandler.init(
            uniResolverUrl = didResolverUrl.orElse(null),
            uniRegistrarUrl = didRegistrarUrl.orElse(null),
            withUniResolver = useUniResolver
        )
    }
}
