package eu.gaiax.notarization.auto_notary.application

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.util.*

@ConfigMapping(prefix = "gaia-x.profile.augmentation")
interface IdentityAugmentationConfig {

    @WithDefault(value = "false")
    fun enabled(): Boolean
    fun decryptionKey(): Optional<String>

    fun claims(): Optional<List<String>>
    @WithDefault(value = "claims.claimsMap.")
    fun ignorePrefix(): String
}
