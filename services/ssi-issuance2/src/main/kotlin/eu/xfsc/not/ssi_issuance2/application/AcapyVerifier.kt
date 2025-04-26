package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ProofVerificationResult
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.xfsc.not.ssi_issuance2.domain.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import mu.KotlinLogging
import openapi.acapy.model.LDProofVCOptions
import openapi.acapy.model.VerifyPresentationRequest
import org.eclipse.microprofile.rest.client.inject.RestClient

private val log = KotlinLogging.logger {}

@ApplicationScoped
@Alternative
class AcapyVerifier : LdpProofVerifier {

    @RestClient
    lateinit var vcApi: FixedAcapyVerifyApi

    override val proofMechanisms = CryptoMechanisms(
        keyTypes = setOf(KeyType.ED25519, KeyType.BLS12381G2),
        signatureTypes = setOf(
            SignatureType.ED25519SIGNATURE2018,
            SignatureType.ED25519SIGNATURE2020,
            SignatureType.BBSBLSSIGNATURE2020,
            SignatureType.BBSBLSSIGNATUREPROOF2020,
        ),
    )
    override fun verify(
        challenge: String,
        domain: String,
        ldpVp: ObjectNode,
        holderDid: String?,
    ): VerifierResult {

        val opts = LDProofVCOptions().apply {
            this.challenge = challenge
            this.domain = domain
        }
        val request = VerifyPresentationRequest().apply {
            options = opts
            verifiablePresentation = ldpVp
        }
        val response = vcApi.vcPresentationsVerifyPost(request) ?: return VerifierFailedResult(
            result = ProofVerificationResult.UNKNOWN_ERROR,
            description = "Validation service didn't answer correctly."
        )

        val holderDidInReport = response.presentationResult.get("document")?.get("holder")

        if (response.verified && holderDidInReport != null) {
            holderDid?.run {
                if (holderDidInReport.asText() != this) {
                    return VerifierFailedResult(
                        result = ProofVerificationResult.WRONG_KEY,
                        description = "Expected holder DID does not match the proof"
                    )
                }
            }

            return VerifierValidResult(
                proofPubKey = holderDidInReport
            )
        } else {
            return VerifierFailedResult(
                result = ProofVerificationResult.UNKNOWN_ERROR,
                description = response.errors.joinToString("\n")
            )
        }
    }
}
