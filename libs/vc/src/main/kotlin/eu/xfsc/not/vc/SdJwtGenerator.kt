package eu.xfsc.not.vc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jwt.JWTClaimNames
import com.nimbusds.jwt.JWTClaimsSet
import id.walt.sdjwt.JWTCryptoProvider
import id.walt.sdjwt.SDJwt
import id.walt.sdjwt.SDMap
import id.walt.sdjwt.SDPayload
import java.util.*


/**
 * @author Mike Prechtl
 */
class SdJwtGenerator {

    private var sdAlgClaim = "_sd_alg"

    private var claimBlacklist : Set<String> = setOf(
        "iss",
        "nbf",
        "exp",
        "cnf",
        "vct",
        "status",
        sdAlgClaim,
    )

    fun createSDJwt(sdJwtInput: SdJwtInput, jwtCryptoProvider: JWTCryptoProvider, keyId: String?) : SDJwt {
        val originalClaimsSet = prepareClaims(sdJwtInput)
        val sdMap = prepareSDMap(sdJwtInput.sdPaths, originalClaimsSet)
        val sdPayload = SDPayload.createSDPayload(originalClaimsSet, sdMap)
        return SDJwt.sign(sdPayload, jwtCryptoProvider, keyId)
    }

    /**
     * Create a JWTClaimSet based on the input data.
     * This method adds some additional required claims, like issuer, issueTime and the type.
     */
    private fun prepareClaims(sdJwtInput: SdJwtInput) : JWTClaimsSet {
        val objMapper = ObjectMapper()
        val jwtOriginalClaimSet = JWTClaimsSet.parse(objMapper.writeValueAsString(sdJwtInput.payload))
        val jwtOriginalClaimBuilder = JWTClaimsSet.Builder(jwtOriginalClaimSet)

        if (! jwtOriginalClaimBuilder.claims.containsKey(JWTClaimNames.ISSUER)) {
            jwtOriginalClaimBuilder.issuer(sdJwtInput.issuerDid)
        }

        if (! jwtOriginalClaimBuilder.claims.containsKey(JWTClaimNames.ISSUED_AT)) {
            jwtOriginalClaimBuilder.issueTime(Date())
        }
        if (! jwtOriginalClaimBuilder.claims.containsKey(JWTClaimNames.NOT_BEFORE)) {
            jwtOriginalClaimBuilder.notBeforeTime(Date())
        }

        jwtOriginalClaimBuilder.claim(sdAlgClaim, "sha-256")

        if (! jwtOriginalClaimBuilder.claims.containsKey("cnf")) {
            val cnfObj = mapOf("jwk" to sdJwtInput.jwk)
            val cnfJsonObject = JWTClaimsSet.parse(objMapper.writeValueAsString(cnfObj)).toJSONObject()
            jwtOriginalClaimBuilder.claim("cnf", cnfJsonObject)
        }

        if (! jwtOriginalClaimBuilder.claims.containsKey("vct")) {
            throw IllegalStateException("Missing required claim 'vct' in input payload")
        }

        return jwtOriginalClaimBuilder.build()
    }

    /**
     * Create a SDMap which indicates the selective disclosure for each field.
     * By default, take all claims excluding the blacklisted ones.
     */
    private fun prepareSDMap(sdPaths: Set<String>?, originalClaimsSet: JWTClaimsSet) : SDMap {
        val filteredSdPaths = (sdPaths ?: originalClaimsSet.claims.map { it.key }.toSet())
            .subtract(claimBlacklist)
        return SDMap.generateSDMap(filteredSdPaths)
    }
}

data class SdJwtInput(
    val payload: JsonNode,
    val issuerDid: String,
    val sdPaths: Set<String>? = null,
    var jwk: JsonNode
)
