package eu.xfsc.not.ssi_issuance2.application

import com.danubetech.keyformats.crypto.PrivateKeySigner
import com.danubetech.keyformats.jose.JWSAlgorithm
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.Profile
import eu.xfsc.not.ssi_issuance2.domain.CryptoMechanisms
import eu.xfsc.not.ssi_issuance2.domain.LdpCredentialIssuer
import eu.xfsc.not.vc.DidHandler
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import id.walt.crypto.keys.Key
import id.walt.crypto.keys.jwk.JWKKey
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords
import info.weboftrust.ldsignatures.signer.LdSignerRegistry
import info.weboftrust.ldsignatures.suites.SignatureSuites
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.net.URI
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger { }

@ApplicationScoped
class LocalLdpCredentialIssuer : LdpCredentialIssuer {

    @Inject
    lateinit var mapper: ObjectMapper

    override val credentialMechanisms = LocalLdpCredentialIssuer.credentialMechanisms

    @Inject
    lateinit var dbKeyManager: DbKeyManager

    @Inject
    lateinit var ldpCredentialBuilder: LdpCredentialBuilder

    override fun createCredential(
        profile: Profile,
        issuerDid: String,
        signatureType: SignatureType,
        issuanceTimestamp: Instant,
        subjectDid: String,
        credentialData: ObjectNode,
    ): JsonNode {
        val (preparedCredentialData, version) = ldpCredentialBuilder.build(
            profile,
            issuerDid,
            issuanceTimestamp,
            credentialData,
            subjectDid,
        )
        val verifiableCredential: JsonLDObject = asJsonLDObject(preparedCredentialData)

        verifiableCredential.documentLoader = documentLoader

        val keyId: String = issuerDid
        val key = dbKeyManager.getKey(keyId) ?: throw IllegalArgumentException("Unknown issuer did $keyId")
        val keySigner = when(key) {
            is JWKKey -> PrivateJWKKeySigner(key)
            else -> PrivateCryptoKeySigner(key)
        }
        val verifyMethod = determineVerifyMethod(issuerDid)
        val ldSigner = LdSignerRegistry.getLdSignerBySignatureSuiteTerm(signatureType.value)

        ldSigner.apply {
            signer = keySigner
            created = Date()
            proofPurpose = LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD
            verificationMethod = URI.create(verifyMethod)
        }
        ldSigner.sign(verifiableCredential)

        return mapper.valueToTree(verifiableCredential.jsonObject)
    }

    private fun determineVerifyMethod(issuerDid: String): String {
        logger.debug { "Resolving did document from did: $issuerDid" }

        val match = isDidKeyMatcher.matchEntire(issuerDid)
        if (match != null) {
            // HACK - waltid has a bug leading to parse errors when resolving most did:keys
            val value = match.groups[1]!!.value
            return "did:key:$value#$value"
        } else {
            val didDocumentResults = DidHandler.resolve(issuerDid)
            if (didDocumentResults.isFailure) {
                throw IllegalArgumentException(
                    "Cannot resolve DID document for issuer did: $issuerDid",
                    didDocumentResults.exceptionOrNull()
                )
            }
            val didDocument = didDocumentResults.getOrNull()
                ?: throw IllegalArgumentException("Cannot resolve DID document for issuer did: $issuerDid")

            val verifyMethod = didDocument["authentication"]?.jsonArray?.get(0)?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Cannot resolve verification method from did document for issuer did: $issuerDid")
            logger.warn { "Verify method: $verifyMethod" }
            return verifyMethod
        }

    }

    override fun canIssueForDid(did: String): Boolean {
        return dbKeyManager.hasDid(did)
    }

    private fun asJsonLDObject(preparedCredentialData: ObjectNode): JsonLDObject {
        val rawData: Map<String, Object> = mapper.convertValue(preparedCredentialData)

        return JsonLDObject.fromJsonObject(rawData)
    }
    companion object {
        val isDidKeyMatcher = Regex("did:key:([^#]+)")

        val credentialMechanisms = CryptoMechanisms(
            keyTypes = DbKeyManager.supportedKeyTypes.keys,
            signatureTypes = SignatureSuites.SIGNATURE_SUITES.map { it.term }
                .filter { LdSignerRegistry.getLdSignerBySignatureSuiteTerm(it) != null }
                .map { item ->
                    SignatureType.entries.first { item == it.value }
                }
                .toSet(),
        )

        private val documentLoader = ConfigurableDocumentLoader()

        init {
            documentLoader.isEnableHttps = true
        }
    }

}

class PrivateJWKKeySigner(privateKey: JWKKey) : PrivateKeySigner<JWKKey>(privateKey, JWSAlgorithm.EdDSA) {
    @Throws(GeneralSecurityException::class)
    override fun sign(content: ByteArray): ByteArray = runBlocking {
        privateKey.signRaw(content)
    }
}

class PrivateCryptoKeySigner(privateKey: Key) : PrivateKeySigner<Key>(privateKey, JWSAlgorithm.EdDSA) {
    @Throws(GeneralSecurityException::class)
    override fun sign(content: ByteArray): ByteArray = runBlocking {
        privateKey.signRaw(content) as ByteArray
    }
}
