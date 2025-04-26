package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ProofVerificationResult
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.xfsc.not.ssi_issuance2.domain.*
import eu.xfsc.not.vc.JwtValidator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject


@ApplicationScoped
class VcLibJwtVerifier : JwtProofVerifier {
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
                SignatureType.ES256,
                SignatureType.ES384,
                SignatureType.ES512,
                SignatureType.ES256K,
                SignatureType.EdDSA,
                SignatureType.RS256,
                SignatureType.RS384,
                SignatureType.RS512,
                SignatureType.PS256,
                SignatureType.PS384,
                SignatureType.PS512,
            ),
        )

    override fun verify(
        jwtStr: String,
        typ: String,
        nonce: String,
        aud: String,
        iss: String?,
        holderDid: String?,
    ): VerifierResult {
        try {
            val result = JwtValidator().validateOidJwtTokenProof(jwtStr, holderDid, typ, iss, aud, nonce)

            return if (result.valid) {
                VerifierValidResult(
                    proofPubKey = om.readTree(result.jwk)
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
