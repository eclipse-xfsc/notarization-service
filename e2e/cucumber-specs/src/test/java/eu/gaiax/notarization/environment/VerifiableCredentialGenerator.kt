package eu.gaiax.notarization.environment

import com.danubetech.verifiablecredentials.CredentialSubject
import com.danubetech.verifiablecredentials.VerifiableCredential
import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.fasterxml.jackson.databind.ObjectMapper
import foundation.identity.jsonld.JsonLDUtils
import id.walt.crypto.keys.KeyType
import id.walt.crypto.keys.jwk.JWKKey
import id.walt.did.dids.DidService
import id.walt.did.dids.registrar.dids.DidKeyCreateOptions
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.js.ExperimentalJsExport


@ApplicationScoped
class VerifiableCredentialGenerator {

    @Inject
    lateinit var mapper: ObjectMapper

    private val webJwk = """
        {
            "kty": "OKP",
            "d": "8fmlXn3y0bGB9hrEP9wtOUz1ibhnvZHcP0K38E5jAR8",
            "crv": "Ed25519",
            "kid": "P2TKlbAIKtdzO4wlM8qOKFO1RRjibDx1nxmr4z-lBTw",
            "x": "pofEEeN-qPCdZ-ZH21IG-Vib43LLlH11UR5TRvQw8q0"
        }
    """
    private val webDidVerifyMethod = "did:web:eid.services:not-api#P2TKlbAIKtdzO4wlM8qOKFO1RRjibDx1nxmr4z-lBTw"

    val webDid = "did:web:eid.services:not-api"

    fun getVerifiablePresentation(nonce: String, useDidWeb: Boolean = false) : String {
        val verifiablePresentationRes: CompletableFuture<String> = getVerifiablePresentationFuture(nonce, useDidWeb)
        return verifiablePresentationRes[10, TimeUnit.SECONDS]
    }

    @OptIn(ExperimentalJsExport::class)
    fun getVerifiablePresentationFuture(nonce: String, useDidWeb: Boolean) : CompletableFuture<String> = GlobalScope.future {
        if (useDidWeb) {
            val key =  JWKKey.importJWK(webJwk).getOrNull() ?: throw IllegalStateException("Unable to import JWK.")
            createVerifiablePresentation(nonce, webDid, webDidVerifyMethod, key)
        } else {
            val localKey = JWKKey.generate(type = KeyType.Ed25519)
            val didOpts = DidKeyCreateOptions(keyType = KeyType.Ed25519, useJwkJcsPub = false)
            val didResult = DidService.registerByKey(method = didOpts.method, key = localKey, options = didOpts)
            val didVerifyMethod = didResult.didDocument["authentication"]?.jsonArray?.get(0)?.jsonPrimitive?.content ?: throw IllegalStateException("No verify method available.")
            val did = didResult.did
            createVerifiablePresentation(nonce, did, didVerifyMethod, localKey)
        }
    }

    @OptIn(ExperimentalJsExport::class)
    @Throws(IllegalArgumentException::class)
    private suspend fun createVerifiablePresentation(nonce: String, did: String, didVerifyMethod: String, localKey: JWKKey) : String {
        DidService.minimalInit()

        // see https://github.com/danubetech/verifiable-credentials-java/blob/main/examples-ldp.md
        val credentialSubject: CredentialSubject = createCredentialSubject(did)
        val verifiableCredential: VerifiableCredential = createCredential(credentialSubject, did)

        val testEd25519PrivateKey = Ed25519EdDSAPrivateKeySigner(localKey)

        // sign verifiable credential
        Ed25519Signature2018LdSigner(testEd25519PrivateKey).apply {
            created = Date()
            proofPurpose = LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD
            verificationMethod = URI.create(didVerifyMethod)
            sign(verifiableCredential)
        }

        // see https://github.com/danubetech/verifiable-credentials-java/blob/main/examples-ldp-vp.md
        val verifiablePresentation = VerifiablePresentation.builder()
            .holder(URI.create(did))
            .verifiableCredential(verifiableCredential)
            .build()

        // sign verifiable presentation
        Ed25519Signature2018LdSigner(testEd25519PrivateKey).apply {
            created = Date()
            proofPurpose = LDSecurityKeywords.JSONLD_TERM_AUTHENTICATION
            verificationMethod = URI.create(didVerifyMethod)
            challenge = nonce
            domain = "localhost"
            sign(verifiablePresentation)
        }

        val verifiablePresentationJson = mapper.readTree(verifiablePresentation.toJson())
        val verifiablePresentationReadable = verifiablePresentationJson.toPrettyString()
        Log.info(verifiablePresentationReadable)
        return verifiablePresentationReadable
    }

    private fun createCredential(credentialSubject: CredentialSubject, issuerDid: String): VerifiableCredential {
        return VerifiableCredential.builder()
            .contexts(listOf(
                URI.create("https://www.w3.org/2018/credentials/v1"),
                URI.create("https://www.w3.org/2018/credentials/examples/v1")
            ))
            .type("VerifiableCredential")
            .id(URI.create("urn:uuid:1b414c1c-e2a2-4185-9b6b-d49a0280830b"))
            .issuer(URI.create(issuerDid))
            .issuanceDate(JsonLDUtils.stringToDate("2019-06-16T18:56:59Z"))
            .credentialSubject(credentialSubject)
            .build()
    }

    private fun createCredentialSubject(did: String) : CredentialSubject {
        val claims: MutableMap<String, Any> = LinkedHashMap()
        claims["name"] = "peter"

        return CredentialSubject.builder()
            .id(URI.create(did))
            .claims(claims)
            .build()
    }
}
