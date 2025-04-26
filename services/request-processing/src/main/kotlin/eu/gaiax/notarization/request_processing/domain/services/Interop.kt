package eu.gaiax.notarization.request_processing.domain.services

import java.security.SecureRandom
import java.util.*

object Interop {

    fun urlSafeString(secureRandom: SecureRandom, tokenBuffer: ByteArray): String {
        secureRandom.nextBytes(tokenBuffer)
        return Base64.getUrlEncoder().encodeToString(tokenBuffer)
    }
}
