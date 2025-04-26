package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.nimbusds.jose.JWSAlgorithm
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.Profile
import eu.xfsc.not.vc.SdJwtGenerator
import eu.xfsc.not.vc.SdJwtInput
import id.walt.crypto.keys.Key
import id.walt.sdjwt.JWTCryptoProvider
import id.walt.sdjwt.JwtVerificationResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import java.time.Instant


@ApplicationScoped
class JwtWaltCryptoProviderBuilder {
    @Inject
    lateinit var dbKeyManager: DbKeyManager

    fun buildProvider(did: String, alg: String): JWTCryptoProvider {
        val sigAlg = JWSAlgorithm.parse(alg)
        val key = dbKeyManager.getKey(did)
            ?: throw NotFoundException("Unable to find key for the provided keyId.")
        return SDJwtCryptoProvider(key, sigAlg)
    }
}

/**
 * @author Mike Prechtl
 */
class SDJwtCryptoProvider(val key: Key, val alg: JWSAlgorithm) : JWTCryptoProvider {

    override fun sign(payload: JsonObject, keyID: String?, typ: String): String {
        val jwsHeaders = mapOf(
            "alg" to alg.toString(),
            "typ" to "vc+sd-jwt",
        )

        return signJws(key, payload, jwsHeaders)
    }

    private fun signJws(key: Key, payload: JsonObject, jwsHeaders: Map<String, String>) = runBlocking {
        key.signJws(payload.toString().encodeToByteArray(), jwsHeaders)
    }

    override fun verify(jwt: String): JwtVerificationResult {
        throw UnsupportedOperationException("Not implemented.")
    }
}


@ApplicationScoped
class SDJwtCredentialIssuer {

    @Inject
    lateinit var cryptoProv: JwtWaltCryptoProviderBuilder

    fun buildPayload(
        profile: Profile,
        issuerDid: String,
        issuanceDate: Instant,
        credData: ObjectNode,
    ): ObjectNode {
        val credToIssue = credData.deepCopy()

        credToIssue.put("iss", issuerDid)
        credToIssue.put("iat", issuanceDate.toEpochMilli() / 1000)

        return credToIssue
    }

    fun createCredential(
        profile: Profile,
        issuerDid: String,
        signatureType: SignatureType,
        issuanceDate: Instant,
        subjectJwk: ObjectNode,
        credentialData: ObjectNode,
    ): JsonNode {
        val preparedCredentialData = buildPayload(
            profile,
            issuerDid,
            issuanceDate,
            credentialData,
        )

        val sdJwtInput = SdJwtInput(
            payload = preparedCredentialData,
            issuerDid = issuerDid,
            // TODO: consider SD-Paths
            null,
            subjectJwk,
        )

        val keyId: String = issuerDid
        val sdJwtGenerator = SdJwtGenerator()
        val sdJwtCryptoProvider = cryptoProv.buildProvider(keyId, signatureType.value)
        val sdJwt = sdJwtGenerator.createSDJwt(sdJwtInput, sdJwtCryptoProvider, keyId)

        return TextNode.valueOf(sdJwt.toString(true))
    }
}
