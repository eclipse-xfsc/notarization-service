package eu.gaiax.notarization.util

import jakarta.enterprise.context.RequestScoped
import java.security.SecureRandom
import java.util.Base64

@RequestScoped
class RandomGenerator {
    private val random = SecureRandom.getInstance("NativePRNGNonBlocking")
    fun genNonce(numBytes: Int = 32): String {
        val bytes = ByteArray(numBytes)
        random.nextBytes(bytes)
        var enc = Base64.getUrlEncoder().encodeToString(bytes)
        return enc
    }
}
