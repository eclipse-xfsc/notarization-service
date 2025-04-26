package eu.xfsc.not.ssi_issuance2.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ProofVerificationResult
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.Profile
import java.time.Instant


interface LdpCredentialIssuer {

    val credentialMechanisms: CryptoMechanisms

    fun createCredential(
        profile: Profile,
        issuerDid: String,
        signatureType: SignatureType,
        issuanceTimestamp: Instant,
        subjectDid: String,
        credentialData: ObjectNode,
    ): JsonNode

    fun canIssueForDid(did: String): Boolean
}

interface LdpProofVerifier {

    val proofMechanisms: CryptoMechanisms

    fun verify(
        challenge: String,
        domain: String,
        ldpVp: ObjectNode,
        holderDid: String?,
    ): VerifierResult

}

interface JwtProofVerifier {

    val proofMechanisms: CryptoMechanisms

    fun verify(
        jwtStr: String,
        typ: String,
        nonce: String,
        aud: String,
        iss: String?,
        holderDid: String?,
    ): VerifierResult

}

sealed interface VerifierResult

data class VerifierValidResult(
    val proofPubKey: JsonNode,
) : VerifierResult

data class VerifierFailedResult(
    val result: ProofVerificationResult,
    val description: String,
) : VerifierResult


class CryptoMechanisms(
    val keyTypes: Set<KeyType>,
    val signatureTypes: Set<SignatureType>,
)
