package eu.xfsc.not.oid4vp.domain

import eu.xfsc.not.oid4vp.Oid4VpConfig
import eu.xfsc.not.oid4vp.model.Oid4VpClientMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class ClientMetadataService {
    @Inject
    lateinit var conf: Oid4VpConfig
    @Inject
    lateinit var ldpVerifier: LdpVerifier
    @Inject
    lateinit var ldpJwtVerifier: LdpJwtVerifier

    fun buildMetadata(): Oid4VpClientMetadata {
        return Oid4VpClientMetadata(
            vpFormats = mapOf(
                ldpVerifier.supportedFormat(),
                ldpJwtVerifier.supportedFormat(),
            )
        )
    }

}
