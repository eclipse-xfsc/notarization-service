package eu.gaiax.notarization.util.db

import io.smallrye.jwt.util.KeyUtils
import jakarta.persistence.AttributeConverter
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwe.JsonWebEncryption
import java.security.Key


interface EncryptionKeyProvider {
    val encKey: Key
    val decKey: Key
}

abstract class JwkKeyProvider: EncryptionKeyProvider {
    abstract protected val jwk: String
    override val encKey: java.security.Key get() {
        val jwks = KeyUtils.loadJsonWebKeys(jwk)
        return jwks?.firstOrNull()?.key
            ?: throw IllegalArgumentException("Key is not a JWK")
    }
    override val decKey get() = encKey
}


interface JweColumnEncryptor : AttributeConverter<String, String> {

    val alg: String
    val enc: String
    val keyProvider: EncryptionKeyProvider

    override fun convertToDatabaseColumn(unencData: String): String {
        try {
            return JsonWebEncryption().apply {
                algorithmHeaderValue = alg
                encryptionMethodHeaderParameter = enc
                setKey(keyProvider.encKey)
                setPlaintext(unencData.toByteArray(Charsets.UTF_8))
            }.compactSerialization
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun convertToEntityAttribute(encDbData: String): String {
        try {
            return JsonWebEncryption().apply {
                setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
                setContentEncryptionAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
                setKey(keyProvider.decKey)
                setCompactSerialization(encDbData)
            }.plaintextBytes.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}
