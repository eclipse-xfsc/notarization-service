package eu.xfsc.not.ssi_issuance2.application

import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.convertValue
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ProofVerificationResult
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.xfsc.not.ssi_issuance2.domain.*
import eu.xfsc.not.vc.ProofValidator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class VcLibLdpVerifier : LdpProofVerifier {

    @Inject
    lateinit var om: ObjectMapper

    override val proofMechanisms: CryptoMechanisms
        get() = CryptoMechanisms(
            keyTypes = setOf(
                KeyType.RSA,
                KeyType.P_256,
                KeyType.P_384,
                KeyType.P_521,
                KeyType.SECP256k1,
                KeyType.ED25519
            ),
            signatureTypes = setOf(
                SignatureType.ED25519SIGNATURE2018,
                SignatureType.ED25519SIGNATURE2020,
                SignatureType.RSASIGNATURE2018,
                SignatureType.JCSED25519SIGNATURE2020,
                SignatureType.ECDSASECP256K1SIGNATURE2019,
                SignatureType.ECDSAKOBLITZSIGNATURE2016,
                SignatureType.JCSECDSASECP256K1SIGNATURE2019,
                SignatureType.JSONWEBSIGNATURE2020,
            ),
        )

    override fun verify(challenge: String, domain: String, ldpVp: ObjectNode, holderDid: String?): VerifierResult {
        try {
            val vp = VerifiablePresentation.fromMap(om.convertValue(ldpVp))
            val result = ProofValidator().validate(vp, challenge = challenge, domain = domain, requireVc = false)

            holderDid?.run {
                if (result.proofValidation.did != this) {
                    return VerifierFailedResult(
                        result = ProofVerificationResult.WRONG_KEY,
                        description = "Expected holder DID does not match the proof"
                    )
                }
            }

            return if (result.valid) {
                VerifierValidResult(
                    proofPubKey = TextNode.valueOf(result.proofValidation.did)
                )
            } else {
                // TODO: obtain better result
                VerifierFailedResult(
                    result = ProofVerificationResult.UNKNOWN_ERROR,
                    description = "Unknown error"
                )
            }
        } catch (e: Exception) {
            val result = ProofVerificationResult.UNKNOWN_ERROR
            return VerifierFailedResult(
                result = result,
                description = e.message ?: "Unknown error"
            )
        }
    }
}
