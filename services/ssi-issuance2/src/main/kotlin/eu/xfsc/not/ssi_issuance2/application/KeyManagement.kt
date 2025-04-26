package eu.xfsc.not.ssi_issuance2.application

import eu.gaiax.notarization.api.issuance.KeyType
import id.walt.crypto.keys.Key

interface KeyInit {
    /**
     * Initialize a new keypair for the given keyType and signatureType.
     * @param keyType The type of the key to be created
     * @return The DID of the newly created keypair
     */
    fun initKeypair(keyType: KeyType): String

    fun hasDid(did: String): Boolean
}

interface KeyAccess {
    fun getKey(did: String): Key?
}

interface KeyManager : KeyInit, KeyAccess
