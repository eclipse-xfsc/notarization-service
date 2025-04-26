package eu.xfsc.not.oid4vp

import io.smallrye.config.ConfigMapping
import java.net.URI
import java.time.Duration
import java.util.*

@ConfigMapping(prefix = "gaia-x.oid4vp")
interface Oid4VpConfig {
    fun baseUrl(): URI
    fun requestObjectLifetime(): Duration

    /**
     * The URI to which the user will be redirected after presenting the VP to the verifier.
     */
    fun finishRedirectUri(): URI

    fun client(): ClientConfig
}

interface ClientConfig {
    fun clientId(): String
    fun scheme(): String
    fun keystore(): KeystoreConfig
    fun jwsAlg(): Optional<String>
}

interface KeystoreConfig {
    fun type(): Optional<String>
    fun provider(): Optional<String>
    fun location(): Optional<String>
    fun alias(): String
    fun password(): String
}
