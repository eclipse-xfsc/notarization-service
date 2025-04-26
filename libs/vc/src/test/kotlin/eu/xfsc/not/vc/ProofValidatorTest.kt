package eu.xfsc.not.vc

import bbs.signatures.Bbs
import com.danubetech.verifiablecredentials.VerifiableCredential
import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.danubetech.verifiablecredentials.jwt.JwtVerifiablePresentation
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.Ed25519Signer
import id.walt.crypto.keys.KeyType
import id.walt.crypto.keys.jwk.JWKKey
import id.walt.did.dids.DidService
import id.walt.did.dids.registrar.dids.DidKeyCreateOptions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junitpioneer.jupiter.ReportEntry
import java.net.URI
import java.time.Instant
import java.util.*


class ProofValidatorTest {

    @BeforeEach
    fun setup() {
        DidHandler.init(withUniResolver = false, withUniRegistrar = false)
    }


    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test VC Good 1`() {
        //val sd: ReportEntryExtension
        val vcData = javaClass.getResourceAsStream("/vc-good-1.json")
            .readAllBytes().decodeToString()
        val vc = VerifiableCredential.fromJson(vcData)
        val result = ProofValidator().validate(vc)
        assertEquals("did:key:z6MkjRagNiMu91DduvCvgEsqLZDVzrJzFrwahc4tXLt9DoHd", result.proofValidation.did)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test VC Good 2`() {
        val vcData = javaClass.getResourceAsStream("/vc-good-2.json")
            .readAllBytes().decodeToString()
        val vc = VerifiableCredential.fromJson(vcData)
        val result = ProofValidator().validate(vc)
        assertEquals("did:key:z6MkrEaHX9iBzCmbSSs84qCRLLGygr5hfMyKA2WTpmBUrjZ1", result.proofValidation.did)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test VP Good 1`() {
        val vcData = javaClass.getResourceAsStream("/vp-good-1.json")
            .readAllBytes().decodeToString()
        val vc = VerifiablePresentation.fromJson(vcData)
        val result = ProofValidator().validate(vc, domain = "issuer.example.com", challenge = "99612b24-63d9-11ea-b99f-4f66f3e4f81a")
        assertEquals("did:key:z6MkjRagNiMu91DduvCvgEsqLZDVzrJzFrwahc4tXLt9DoHd", result.proofValidation.did)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test VP Good 2`() {
        val vcData = javaClass.getResourceAsStream("/vp-good-2.json")
            .readAllBytes().decodeToString()
        val vc = VerifiablePresentation.fromJson(vcData)
        val result = ProofValidator().validate(vc, challenge = "ABC")
        assertEquals("did:key:z6MkiVqUZA11fPeMSRDKPjReSXBAN4xxXDkXjGPjzmZqhmhQ", result.proofValidation.did)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test VP Bad 1`() {
        val vcData = javaClass.getResourceAsStream("/vp-bad-1.json")
            .readAllBytes().decodeToString()
        val vc = VerifiablePresentation.fromJson(vcData)
        assertThrows<NullPointerException> {
            ProofValidator().validate(vc, challenge = "ABC")
        }
    }

    // disabled because BBS library is not loaded properly
    //@Test
    fun `test VP Good 3`() {
        Bbs.getSignatureSize()
        val vcData = javaClass.getResourceAsStream("/vp-good-3.json")
            .readAllBytes().decodeToString()
        val vc = VerifiablePresentation.fromJson(vcData)
        ProofValidator().validate(vc)
    }

    // this test credential is bullshit, so the test is disabled until a suitable replacement is found
    // note that the contained ebsi Credential Attestation is used without a context definition, which is probably also invalid
    //@Test
    fun `test VP Good 4 JWT`() {
        DidHandler.init(withUniResolver = true, withUniRegistrar = false)

        // example data from https://hub.ebsi.eu/conformance/build-solutions/holder-wallet-functional-flows
        val vpStr = javaClass.getResourceAsStream("/vp-good-4.jwt")
            .readAllBytes()
            .decodeToString()
            .replace("\n", "")
        val presSub = """
            {
              "id": "d6c0b497-bed9-4c5f-9929-de31fec0adf0",
              "definition_id": "holder-wallet-qualification-presentation",
              "descriptor_map": [
                {
                  "id": "same-device-authorised-in-time-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "same-device-authorised-in-time-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[0]"
                  }
                },
                {
                  "id": "cross-device-authorised-in-time-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "cross-device-authorised-in-time-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[1]"
                  }
                },
                {
                  "id": "same-device-authorised-deferred-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "same-device-authorised-deferred-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[2]"
                  }
                },
                {
                  "id": "cross-device-authorised-deferred-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "cross-device-authorised-deferred-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[3]"
                  }
                },
                {
                  "id": "same-device-pre-authorised-in-time-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "same-device-pre-authorised-in-time-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[4]"
                  }
                },
                {
                  "id": "cross-device-pre-authorised-in-time-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "cross-device-pre-authorised-in-time-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[5]"
                  }
                },
                {
                  "id": "same-device-pre-authorised-deferred-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "same-device-pre-authorised-deferred-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[6]"
                  }
                },
                {
                  "id": "cross-device-pre-authorised-deferred-credential",
                  "path": "${'$'}",
                  "format": "jwt_vp",
                  "path_nested": {
                    "id": "cross-device-pre-authorised-deferred-credential",
                    "format": "jwt_vc",
                    "path": "${'$'}.vp.verifiableCredential[7]"
                  }
                }
              ]
            }
        """.trimIndent()
        val vpJwt = JwtVerifiablePresentation.fromCompactSerialization(vpStr)
        val validationDate = Instant.parse("2020-05-17T07:07:40Z")
        val result = ProofValidator().validate(
            vpJwt,
            domain = "https://api-conformance.ebsi.eu/conformance/v3/auth-mock",
            challenge = "FgkeErf91kfl",
            validationDate = validationDate
        )
        assertEquals("did:key:z6MkjRagNiMu91DduvCvgEsqLZDVzrJzFrwahc4tXLt9DoHd", result.proofValidation.did)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test VP gen JWT`() = runBlocking {
        val localKey = JWKKey.generate(type = KeyType.Ed25519)
        val didOpts = DidKeyCreateOptions(keyType = KeyType.Ed25519, useJwkJcsPub = false)
        val didResult = DidService.registerByKey(method = didOpts.method, key = localKey, options = didOpts)
        val did = didResult.did
        val verifMethod = didResult.didDocument["authentication"]?.jsonArray?.get(0)?.jsonPrimitive?.content
        val joseJwk = com.nimbusds.jose.jwk.JWK.parse(localKey.exportJWK())

        val jwtId = URI.create("urn:uuid:${UUID.randomUUID()}")
        val vp = VerifiablePresentation.builder()
            .id(jwtId)
            .holder(URI.create(did))
            .build()

        val vpMap = vp.toMap()
        val claims = mapOf(
            "vp" to vpMap,
            "iss" to did,
            "nbf" to Date().time / 1000,
        )
        val jwsObject = JWSObject(
            JWSHeader(com.nimbusds.jose.JWSAlgorithm.EdDSA, JOSEObjectType.JWT, null, null, null, null, null, null, null, null, null, true, null, null),
            Payload(claims)
        )
        val signer = Ed25519Signer(joseJwk.toOctetKeyPair())
        jwsObject.sign(signer)
        val vpStr = jwsObject.serialize()

        val vpJwt = JwtVerifiablePresentation.fromCompactSerialization(vpStr)
        val result = ProofValidator().validate(vpJwt, requireVc = false)
        assertEquals(did, result.proofValidation.did)
    }

    suspend fun genJwt(iss: String, nonce: String, typ: String, aud: String, withJwk: Boolean = false, withKid: Boolean = false): JwtCreationResult {
        val localKey = JWKKey.generate(type = KeyType.Ed25519)
        val didOpts = DidKeyCreateOptions(keyType = KeyType.Ed25519, useJwkJcsPub = false)
        val didResult = DidService.registerByKey(method = didOpts.method, key = localKey, options = didOpts)
        val did = didResult.did
        val verifMethod = didResult.didDocument["authentication"]?.jsonArray?.get(0)?.jsonPrimitive?.content!!
        val joseJwk = com.nimbusds.jose.jwk.JWK.parse(localKey.exportJWK())

        val claims = mapOf(
            "iss" to iss,
            "iat" to Date().time / 1000,
            "nonce" to nonce,
            "aud" to arrayOf("some other audience", aud),
        )
        val jwkParam = if (withJwk) joseJwk.toPublicJWK() else null
        val kid = if (withKid) did else null
        val jwsObject = JWSObject(
            JWSHeader(com.nimbusds.jose.JWSAlgorithm.EdDSA, JOSEObjectType(typ), null, null, null, jwkParam, null, null, null, null, kid, true, null, null),
            Payload(claims)
        )
        val signer = Ed25519Signer(joseJwk.toOctetKeyPair())
        jwsObject.sign(signer)
        val vpStr = jwsObject.serialize()

        return JwtCreationResult(vpStr, did, verifMethod)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test gen OID JWT w-kid`() = runBlocking {
        val iss = "some client id"
        val nonce = "abcabcfe"
        val typ = "openid4vci-proof+jwt"
        val aud = "some audience"

        val jwtResult = genJwt(iss, nonce, typ, aud, withKid = true)
        val did = jwtResult.did

        val result = JwtValidator().validateOidJwtTokenProof(jwtResult.jwt, holder = did, type = typ, issuer = iss, aud = aud, nonce = nonce)
        assertEquals(did, result.did)
        assertEquals(jwtResult.verifMethod, result.keyId)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test gen OID JWT w-jwk`() = runBlocking {
        val iss = "some client id"
        val nonce = "abcabcfe"
        val typ = "openid4vci-proof+jwt"
        val aud = "some audience"

        val jwtResult = genJwt(iss, nonce, typ, aud, withJwk = true)
        val did = jwtResult.did

        val result = JwtValidator().validateOidJwtTokenProof(jwtResult.jwt, holder = did, type = typ, issuer = iss, aud = aud, nonce = nonce)
        assertEquals(did, result.did)
        assertEquals(jwtResult.verifMethod, result.keyId)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test GO-lib OID JWT`() = runBlocking {
        val jwt = "eyJhbGciOiJFUzI1NiIsImp3ayI6eyJhbGciOiJFUzI1NiIsImNydiI6IlAtMjU2Iiwia2lkIjoidGVzdCIsImt0eSI6IkVDIiwieCI6IlVsNnprR1Y2NkhmN3M5bzZkM1pZVnNGaHVVMW84SEdZeE1Gd2Z1VWtoek0iLCJ5IjoiaWtVWVFGdnB0UW13aDF6aDA5STQyX0FnSEJ0Rno3RzNadTh6ZjlLU0hzNCJ9LCJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCJ9.eyJhdWQiOlsiaHR0cHM6Ly90ZXN0LW5vdC54ZnNjLmRldi9vaWQ0dmNpLyJdLCJpYXQiOjE3MjEyMjkzMjYsIm5vbmNlIjoiRHdnVG1OOXgyMk1MRU5hOV9wb0N4YzdvQVM0Ul9scWVBeGhLZnU0SVNoST0ifQ.9uzgu4dzc0EaS6IH2N69RSzSyn0Uk-P3HCvUmcqAsy2yo13ZP3a1IZ28gMMwoTpXQ1FkPD2aKHT2wTASRSn6VA"
        val nonce = "DwgTmN9x22MLENa9_poCxc7oAS4R_lqeAxhKfu4IShI="
        val typ = "openid4vci-proof+jwt"
        val aud = "https://test-not.xfsc.dev/oid4vci/"
        val valDate = Instant.parse("2024-07-17T15:15:40Z")

        val result = JwtValidator().validateOidJwtTokenProof(jwt, holder = null, type = typ, aud = aud, nonce = nonce, issuer = null, validationDate = valDate)
        assertTrue(result.valid)
    }
}

class JwtCreationResult(
    val jwt: String,
    val did: String,
    val verifMethod: String,
)
