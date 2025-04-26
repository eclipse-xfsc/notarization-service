package eu.gaiax.notarization.environment

import com.danubetech.keyformats.crypto.PrivateKeySigner
import com.danubetech.keyformats.jose.JWSAlgorithm
import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.Ed25519Signer
import id.walt.crypto.keys.KeyType
import id.walt.crypto.keys.jwk.JWKKey
import id.walt.crypto.utils.encodeToBase58String
import id.walt.did.dids.DidService
import id.walt.did.dids.registrar.dids.DidKeyCreateOptions
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.signer.JsonWebSignature2020LdSigner
import info.weboftrust.ldsignatures.signer.LdSigner
import info.weboftrust.ldsignatures.suites.SignatureSuite
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.js.ExperimentalJsExport


@ApplicationScoped
class ProofGenerator {

    @Inject
    lateinit var mapper: ObjectMapper

    @Inject
    lateinit var holderManagement: HolderManagement

    private lateinit var localKey: JWKKey
    private lateinit var verifyMethod: String
    private lateinit var did: String

    private val webJwk = """
        {
            "kty": "OKP",
            "d": "8fmlXn3y0bGB9hrEP9wtOUz1ibhnvZHcP0K38E5jAR8",
            "crv": "Ed25519",
            "kid": "P2TKlbAIKtdzO4wlM8qOKFO1RRjibDx1nxmr4z-lBTw",
            "x": "pofEEeN-qPCdZ-ZH21IG-Vib43LLlH11UR5TRvQw8q0"
        }
    """
    private val webDid = "did:web:eid.services:not-api"
    private val webDidVerifyMethod = "did:web:eid.services:not-api#P2TKlbAIKtdzO4wlM8qOKFO1RRjibDx1nxmr4z-lBTw"

    fun getDid(didMethod: String) : String {
        val didResult: CompletableFuture<String> = prepareDid(didMethod)
        return didResult[10, TimeUnit.SECONDS]
    }

    private fun prepareDid(didMethod: String) : CompletableFuture<String> = GlobalScope.future {
        createDid(didMethod)
    }

    @OptIn(ExperimentalJsExport::class)
    private suspend fun createDid(didMethod: String) : String {
        DidService.minimalInit()

        if (didMethod == "key") {
            this.localKey = JWKKey.generate(type = KeyType.Ed25519)
            val didOpts = DidKeyCreateOptions(keyType = KeyType.Ed25519, useJwkJcsPub = false)
            val didResult = DidService.registerByKey(method = didOpts.method, key = localKey, options = didOpts)

            this.verifyMethod = didResult.didDocument["authentication"]?.jsonArray?.get(0)?.jsonPrimitive?.content!!
            this.did = didResult.did
            return didResult.did
        } else if (didMethod == "web") {
            this.localKey =
                JWKKey.importJWK(webJwk).getOrNull() ?: throw IllegalStateException("Unable to import JWK.")
            this.verifyMethod = webDidVerifyMethod
            this.did = webDid
            return did
        } else if (didMethod == "sov") {
            this.localKey = JWKKey.generate(type = KeyType.Ed25519)
            val verKey = localKey.getPublicKeyRepresentation().encodeToBase58String()
            val resp = holderManagement.registerAtIndico("3wL51Wz5hL4mQEvSTABTVp", verKey)

            // TODO get verifyMethod from response above, but verKey not correct formatted currently?
            this.verifyMethod = ""
            this.did = "did:sov:testnet:3wL51Wz5hL4mQEvSTABTVp"
            return did
        // TODO: Add support "ebsi" method
        } else {
            throw IllegalStateException("Unsupported did method: $didMethod.")
        }
    }

    fun getProof(nonce: String, credIssuer: String) : String {
        val proofResult: CompletableFuture<String> = prepareProof(nonce, credIssuer)
        return proofResult[10, TimeUnit.SECONDS]
    }

    private fun prepareProof(nonce: String, credIssuer: String) : CompletableFuture<String> = GlobalScope.future {
        createProof(nonce, credIssuer)
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun createProof(nonce: String, credIssuer: String) : String {
        val verifiablePresentation = VerifiablePresentation.builder()
            .holder(URI.create(did))
            .build()

        val testEd25519PrivateKey = Ed25519EdDSAPrivateKeySigner(localKey)

        Ed25519Signature2018LdSigner(testEd25519PrivateKey).apply {
            created = Date()
            proofPurpose = LDSecurityKeywords.JSONLD_TERM_AUTHENTICATION
            verificationMethod = URI.create(verifyMethod)
            challenge = nonce
            domain = credIssuer
            sign(verifiablePresentation)
        }

        val ldpVpProof = mapper.readTree(verifiablePresentation.toJson())
        val proof = ldpVpProof.toPrettyString()

        Log.info(proof)
        return proof
    }

    fun getJWTProof(nonce: String, credIssuer: String, audience: String) : String {
        val proofResult: CompletableFuture<String> = prepareJWTProof(nonce, credIssuer, audience)
        return proofResult[10, TimeUnit.SECONDS]
    }

    private fun prepareJWTProof(nonce: String, credIssuer: String, audience: String) : CompletableFuture<String> = GlobalScope.future {
        createJWTProof(nonce, credIssuer, audience)
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun createJWTProof(nonce: String, credIssuer: String, audience: String, withJwk: Boolean = true, withKid: Boolean = false) : String {
        val joseJwk = com.nimbusds.jose.jwk.JWK.parse(this.localKey.exportJWK())

        val claims = mapOf(
            "iss" to credIssuer,
            "iat" to Date().time / 1000,
            "nonce" to nonce,
            "aud" to audience,
        )
        val jwkParam = if (withJwk) joseJwk.toPublicJWK() else null
        val kid = if (withKid) did else null
        val jwsObject = JWSObject(
            JWSHeader(com.nimbusds.jose.JWSAlgorithm.EdDSA, JOSEObjectType("openid4vci-proof+jwt"), null, null, null, jwkParam, null, null, null, null, kid, true, null, null),
            Payload(claims)
        )
        val signer = Ed25519Signer(joseJwk.toOctetKeyPair())
        jwsObject.sign(signer)
        val vpStr = jwsObject.serialize()

        Log.info(vpStr)
        return vpStr
    }

    fun getJsonWebSignature2020Proof(nonce: String, credIssuer: String, audience: String) : String {
        val proofResult: CompletableFuture<String> = prepareJsonWebSignature2020Proof(nonce, credIssuer, audience)
        return proofResult[10, TimeUnit.SECONDS]
    }

    private fun prepareJsonWebSignature2020Proof(nonce: String, credIssuer: String, audience: String) : CompletableFuture<String> = GlobalScope.future {
        createJsonWebSignature2020Proof(nonce, credIssuer, audience)
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun createJsonWebSignature2020Proof(nonce: String, credIssuer: String, audience: String, withJwk: Boolean = true, withKid: Boolean = false) : String {
        val verifiablePresentation = VerifiablePresentation.builder()
            .holder(URI.create(did))
            .build()

        val testEd25519PrivateKey = Ed25519EdDSAPrivateKeySigner(localKey)

        JsonWebSignature2020LdSigner(testEd25519PrivateKey).apply {
            created = Date()
            proofPurpose = LDSecurityKeywords.JSONLD_TERM_AUTHENTICATION
            verificationMethod = URI.create(verifyMethod)
            challenge = nonce
            domain = credIssuer
            sign(verifiablePresentation)
        }

        val ldpVpProof = mapper.readTree(verifiablePresentation.toJson())
        val proof = ldpVpProof.toPrettyString()

        Log.info(proof)
        return proof
    }
}

class Ed25519EdDSAPrivateKeySigner(privateKey: JWKKey) : PrivateKeySigner<JWKKey>(privateKey, JWSAlgorithm.EdDSA) {
    @Throws(GeneralSecurityException::class)
    override fun sign(content: ByteArray): ByteArray = runBlocking {
        privateKey.signRaw(content)
    }
}
