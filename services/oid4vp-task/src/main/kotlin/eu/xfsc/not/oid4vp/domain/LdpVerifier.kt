package eu.xfsc.not.oid4vp.domain

import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.danubetech.verifiablecredentials.jwt.JwtVerifiablePresentation
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.xfsc.not.oid4vp.model.VpFormatLdp
import eu.xfsc.not.oid4vp.model.VpFormatType
import eu.xfsc.not.vc.ProofValidator
import eu.xfsc.not.vc.VpValidationResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

fun supportedAlgorithms(): List<String> {
    return listOf(
        "RsaSignature2018",
        "Ed25519Signature2018",
        "Ed25519Signature2020",
        "JcsEd25519Signature2020",
        "EcdsaSecp256k1Signature2019",
        "EcdsaKoblitzSignature2016",
        "JcsEcdsaSecp256k1Signature2019",
        "JsonWebSignature2020",
//            "BbsBlsSignature2020",
    )
}

@ApplicationScoped
class LdpVerifier {
    @Inject
    lateinit var om: ObjectMapper


    fun supportedFormat(): Pair<VpFormatType, VpFormatLdp> {
        return VpFormatType.LDP_VP to VpFormatLdp(
            proofType = supportedAlgorithms()
        )
    }

    @Throws(ResponseValidationException::class)
    fun validateVp(vpObj: JsonNode, clientId: String, nonce: String): VpValidationResult {
        val vp = VerifiablePresentation.fromJson(om.writeValueAsString(vpObj))

        // validate proof
        try {
            val result = ProofValidator().validate(vp, domain = clientId, challenge = nonce)
            return result
        } catch (e: Exception) {
            throw ResponseValidationException(ResponseValidationErrorCode.InvalidVp, e.message, e)
        }

    }
}

@ApplicationScoped
class LdpJwtVerifier {
    @Inject
    lateinit var om: ObjectMapper

    fun supportedFormat(): Pair<VpFormatType, VpFormatLdp> {
        return VpFormatType.JWT_VP to VpFormatLdp(
            proofType = supportedAlgorithms()
        )
    }

    @Throws(ResponseValidationException::class)
    fun validateVp(vpObj: JsonNode, clientId: String, nonce: String): VpValidationResult {
        val vp = JwtVerifiablePresentation.fromCompactSerialization(vpObj.textValue())

        // validate proof
        try {
            val result = ProofValidator().validate(vp, domain = clientId, challenge = nonce)
            return result
        } catch (e: Exception) {
            throw ResponseValidationException(ResponseValidationErrorCode.InvalidVp, e.message)
        }

    }
}
