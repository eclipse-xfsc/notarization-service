package eu.xfsc.not.oid4vci

import io.smallrye.config.ConfigMapping
import java.net.URI
import java.time.Duration
import java.util.*

@ConfigMapping(prefix = "gaia-x.oid4vci")
interface Oid4vciConfig {
    fun issuerUrl(): URI
    fun encryptTokens(): Boolean
    fun codeLifetime(): Duration
    fun atLifetime(): Duration
    fun issuerDisplay(): Optional<String>
}
