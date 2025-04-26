package eu.xfsc.not.ssi_issuance2.application

import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.util.db.JweColumnEncryptor
import eu.gaiax.notarization.util.db.JwkKeyProvider
import eu.xfsc.not.vc.DidHandler
import id.walt.crypto.keys.Key
import id.walt.crypto.keys.KeyGenerationRequest
import id.walt.crypto.keys.KeySerialization
import id.walt.did.dids.registrar.dids.DidKeyCreateOptions
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.*
import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import id.walt.crypto.keys.KeyManager as WaltKeyManager

private val logger = KotlinLogging.logger { }

@ApplicationScoped
class DbKeyManager : KeyManager {

    @Inject
    lateinit var keypairRepository: KeypairRepository

    override fun initKeypair(keyType: KeyType): String = runBlocking {
        val waltKt: id.walt.crypto.keys.KeyType = supportedKeyTypes[keyType] ?: throw IllegalArgumentException("Unsupported key type: $keyType")

        val key = WaltKeyManager.createKey(KeyGenerationRequest(backend = "jwk", keyType = waltKt))

        // save key to db
        val keyData = KeySerialization.serializeKey(key)
        val did = registerDidKey(key)
        keypairRepository.saveKeyData(did, keyData)

        // return did for profile service
        did
    }

    private fun registerDidKey(key: Key): String {
        val options = DidKeyCreateOptions(keyType = key.keyType)
        return DidHandler.registerDidKey(key, options).did
    }

    override fun getKey(keyId: String): Key? = runBlocking {
        keypairRepository.retrieveKeyData(keyId)?.let { keyPair ->
            val key = WaltKeyManager.resolveSerializedKey(keyPair.keyData)
            key
        }
    }

    override fun hasDid(did: String): Boolean {
        return keypairRepository.hasDid(did)
    }

    companion object {
        val supportedKeyTypes = mapOf(
            KeyType.ED25519 to id.walt.crypto.keys.KeyType.Ed25519,
            KeyType.SECP256k1 to id.walt.crypto.keys.KeyType.secp256k1,
            KeyType.P_256 to id.walt.crypto.keys.KeyType.secp256r1,
            KeyType.RSA to id.walt.crypto.keys.KeyType.RSA,
        )
    }
}

@Entity(name = "LocalKeyPair")
@Table(name = "local_key_pair")
class LocalKeyPair {
    @Id
    lateinit var did: String
    @Convert(converter = CryptoConverter::class)
    @Column(name = "key_data")
    lateinit var keyData: String
}

@ApplicationScoped
class KeypairRepository : PanacheRepositoryBase<LocalKeyPair, String> {

    @Transactional
    fun saveKeyData(did: String, keyData: String) {
        val keypair = LocalKeyPair()
        keypair.did = did
        keypair.keyData = keyData
        logger.debug { "Adding key for did: $did" }
        persist(keypair)
    }

    @Transactional
    fun retrieveKeyData(keyId: String): LocalKeyPair? {
        return findById(keyId)
    }

    @Transactional
    fun hasDid(did: String): Boolean {
        return find("id = ?1", did).count() == 1L
    }
}


@ApplicationScoped
@Converter
class CryptoConverter : JweColumnEncryptor {
    @ConfigProperty(name = "gaia-x.keymanager.local.alg")
    override lateinit var alg: String
    @ConfigProperty(name = "gaia-x.keymanager.local.enc")
    override lateinit var enc: String
    @Inject
    override lateinit var keyProvider: KeyManagerJwkKeyProvider
}


@ApplicationScoped
class KeyManagerJwkKeyProvider : JwkKeyProvider() {
    @ConfigProperty(name = "gaia-x.keymanager.local.jwk")
    override lateinit var jwk: String
}
