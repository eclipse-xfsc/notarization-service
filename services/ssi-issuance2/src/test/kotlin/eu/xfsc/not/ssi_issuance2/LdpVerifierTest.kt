package eu.xfsc.not.ssi_issuance2

import com.danubetech.keyformats.crypto.PrivateKeySigner
import com.danubetech.keyformats.jose.JWSAlgorithm
import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.xfsc.not.ssi_issuance2.domain.LdpProofVerifier
import eu.xfsc.not.ssi_issuance2.domain.VerifierValidResult
import eu.xfsc.not.ssi_issuance2.mock.AcapyServiceMock
import eu.xfsc.not.ssi_issuance2.mock.TestHelper
import id.walt.crypto.keys.KeyType
import id.walt.crypto.keys.jwk.JWKKey
import id.walt.did.dids.DidService
import id.walt.did.dids.registrar.dids.DidKeyCreateOptions
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.signer.JsonWebSignature2020LdSigner
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.net.URI
import java.security.GeneralSecurityException
import java.util.*

private val log = KotlinLogging.logger {}

@QuarkusTest
@QuarkusTestResource(AcapyServiceMock::class)
class LdpVerifierTest {

    @Inject
    lateinit var verifier: LdpProofVerifier

    @Inject
    lateinit var mapper: ObjectMapper

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun testAcapyVerificationOfProof() {
        val proof = mapper.readTree(TestHelper.PROOF) as ObjectNode
        val result = verifier.verify(TestHelper.PROOF_CHALLENGE, TestHelper.PROOF_DOMAIN, proof, TestHelper.HOLDER_DID)

        log.debug("verifier result: ${result}")

        assertThat("result is not a Valid result", result is VerifierValidResult )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun testJsonWebSignature2020Proof() {
        val proof = mapper.readTree(TestHelper.JSON_WEB_SIGNATURE_PROOF) as ObjectNode
        val result = verifier.verify(TestHelper.PROOF_CHALLENGE, TestHelper.PROOF_DOMAIN, proof, TestHelper.JSON_WEB_SIGNATURE_PROOF_HOLDER)

        log.debug { "verifier result: $result" }

        assertThat("result is not a Valid result", result is VerifierValidResult )
    }

    fun generateJsonWebSignature2020Proof() = runBlocking {
        val localKey = JWKKey.generate(type = KeyType.Ed25519)
        val didOpts = DidKeyCreateOptions(keyType = KeyType.Ed25519, useJwkJcsPub = false)
        val didResult = DidService.registerByKey(method = didOpts.method, key = localKey, options = didOpts)

        val verifyMethod = didResult.didDocument["authentication"]?.jsonArray?.get(0)?.jsonPrimitive?.content!!
        val did = didResult.did

        val verifiablePresentation = VerifiablePresentation.builder()
            .holder(URI.create(did))
            .build()

        val testEd25519PrivateKey = Ed25519EdDSAPrivateKeySigner(localKey)

        JsonWebSignature2020LdSigner(testEd25519PrivateKey).apply {
            created = Date()
            proofPurpose = LDSecurityKeywords.JSONLD_TERM_AUTHENTICATION
            verificationMethod = URI.create(verifyMethod)
            challenge = TestHelper.PROOF_CHALLENGE
            domain = TestHelper.PROOF_DOMAIN
            sign(verifiablePresentation)
        }

        val ldpVpProof = mapper.readTree(verifiablePresentation.toJson())
        val proof = ldpVpProof.toPrettyString()
        log.info { proof }
    }

    class Ed25519EdDSAPrivateKeySigner(privateKey: JWKKey) : PrivateKeySigner<JWKKey>(privateKey, JWSAlgorithm.EdDSA) {
        @Throws(GeneralSecurityException::class)
        override fun sign(content: ByteArray): ByteArray = runBlocking {
            privateKey.signRaw(content)
        }
    }
}
