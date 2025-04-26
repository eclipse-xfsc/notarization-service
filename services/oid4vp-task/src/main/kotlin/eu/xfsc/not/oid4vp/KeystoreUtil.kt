package eu.xfsc.not.oid4vp

import io.smallrye.jwt.util.KeyUtils
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.RequestScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import java.security.KeyStore.PrivateKeyEntry
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*
import kotlin.jvm.optionals.getOrNull


annotation class ClientKeystore

@ApplicationScoped
class ClientKeystoreProvider {
    @Inject
    lateinit var conf: Oid4VpConfig

    @RequestScoped
    @Produces
    @ClientKeystore
    @Throws(Exception::class)
    fun loadKeystore() : KeyStore {
        val keyConf = conf.client().keystore()
        val ks = KeyStore.load(keyConf)
        return ks
    }
}


class KeyStore (
    val keyConf: KeystoreConfig,
    protected val ks: java.security.KeyStore,
) {
    val alias: String
    protected val pass: String
    init {
        alias = keyConf.alias()
        pass = keyConf.password()
    }

    @Throws(Exception::class)
    fun getEntry(): Pair<PrivateKey, X509Certificate> {
        val e = ks.getEntry(alias, java.security.KeyStore.PasswordProtection(pass.toCharArray()))
        when (e) {
            is PrivateKeyEntry -> return Pair(e.privateKey, e.certificate as X509Certificate)
            else -> throw IllegalStateException("KeyStore entry for alias $alias is not a private key")
        }
    }

    companion object {
        fun load(keyConf: KeystoreConfig): KeyStore {
            val ks = KeyUtils.loadKeyStore(
                keyConf.location().getOrNull(),
                keyConf.password(),
                keyConf.type(),
                keyConf.provider()
            )
            return KeyStore(keyConf, ks)
        }
    }
}
