package eu.gaiax.notarization

import eu.xfsc.not.vc.status.StatusList2021Util
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject

class StatusListUtilProducer {
    @Inject
    lateinit var config: RevocationConfig

    @Produces
    fun getStatusList2021Utils(): StatusList2021Util {
        return StatusList2021Util(
            bitstringBlockSize = config.bitstringBlockSize(),
            bitstringMinBlocks = config.bitstringMinBlocks(),
        )
    }
}
