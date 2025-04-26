package eu.xfsc.not.vc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import id.walt.sdjwt.JWTCryptoProvider
import id.walt.sdjwt.SimpleJWTCryptoProvider
import io.quarkus.logging.Log
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry


/**
 * @author Mike Prechtl
 */
class SdJwtGeneratorTest {

    private var sdJWTPayloadInput = """
        {
            "vct": "https://credentials.example.com/identity_credential",
            "type": "IdentityCredential",
            "given_name": "John Markovic  Özcalan",
            "family_name": "Doe",
            "gender": "Male",
            "address": {
                "street": "123 Main St",
                "city": "Anytown",
                "state": "CA",
                "zip": "12345"
            }
        }
    """

    private var sdJwtJWK = """
        {
          "kty": "EC",
          "crv": "P-256",
          "x": "TCAER19Zvu3OHF4j4W4vfSVoHIP1ILilDls7vCeGemc",
          "y": "ZxjiWWbZMQGHVWKVQ4hbSIirsVfuecCE6t4jT9F2HZQ"
        }
    """

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test SD-JWT Generation with HS256`() {
        val sharedSecret = "ef23f749-7238-481a-815c-f0c2157dfa8e"
        val cryptoProvider = SimpleJWTCryptoProvider(JWSAlgorithm.HS256, MACSigner(sharedSecret), MACVerifier(sharedSecret))

        testSDJwtGeneration(cryptoProvider, sdJwtJWK)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test SD-JWT Generation with P-256`() {
        val ecJWK = ECKeyGenerator(Curve.P_256).keyID("123").generate()
        val ecPublicJWK = ecJWK.toPublicJWK()
        val cryptoProvider = SimpleJWTCryptoProvider(JWSAlgorithm.ES256, ECDSASigner(ecJWK), ECDSAVerifier(ecPublicJWK))

        testSDJwtGeneration(cryptoProvider, ecPublicJWK.toJSONString())
    }

    private fun testSDJwtGeneration(cryptoProvider: JWTCryptoProvider, cnfValue: String) {
        // SD-JWT Input Generation data
        val sdJwtInput = SdJwtInput(
            createJsonNode(sdJWTPayloadInput),
            "did:example:123456",
            setOf("given_name", "family_name", "address.city"),
            //null,
            createJsonNode(cnfValue)
        )

        // SD-JWT Generation
        val sdJwtGenerator = SdJwtGenerator()
        val sdJwt = sdJwtGenerator.createSDJwt(sdJwtInput, cryptoProvider, null)

        // SD-JWT Verification
        val sdJwtVerifier = SdJwtVerifier()
        val verificationResult = sdJwtVerifier.verifySdJwt(sdJwt.toString(), cryptoProvider)

        Log.info(sdJwt)
        Log.info(verificationResult)

        //val presentedDisclosedJwt = sdJwt.present(discloseAll = false)
        //Log.info(presentedDisclosedJwt)

        Assertions.assertTrue(verificationResult.signatureVerified)
        Assertions.assertTrue(verificationResult.verified)
    }

    private fun createJsonNode(jsonStr: String) : JsonNode {
        val mapper = ObjectMapper()
        return mapper.readTree(jsonStr)
    }
}
