package eu.xfsc.not.ssi_issuance2.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.issuance.IssuanceInitRequest
import eu.gaiax.notarization.api.issuance.IssuanceInitResponse
import io.restassured.RestAssured
import io.restassured.http.ContentType
import java.net.URI
import java.time.Instant
import java.util.*

object TestHelper {
    const val HOLDER_DID = "did:key:z6MkkXhRtqiRsPK66xWM6mfZTaD1ApRar5gTwXn1S5qbyEbp"
    const val ISSUER_DID = "did:key:z6MkkoJ8hmunDiaAWkTPsLvCaU7BR7cM3dyzJYGH2UpxaDsK"
    const val CRED_DATA = """{
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://w3id.org/citizenship/v1"
        ],
        "type": [
            "VerifiableCredential",
            "PermanentResidentCard"
        ],
        "credentialSubject" : {
            "type": "PermanentResident",
            "image": "data:image/png;base64,iVBORw0KGgo...kJggg==",
            "gender": "Male",
            "birthDate": "1958-07-17",
            "givenName": "e1ab4271-6865-43e8-bdc6-0e0157617e2c",
            "lprNumber": "999-999-999",
            "familyName": "SMITH",
            "lprCategory": "C09",
            "birthCountry": "Bahamas",
            "residentSince": "2015-01-01",
            "commuterClassification": "C1"
            }
        }"""
    const val PROOF_CHALLENGE = "c_nonce value"
    const val PROOF_DOMAIN = "https://oid4vci.issuer.com/"
    const val PROOF = """{
            "@context" : [ "https://www.w3.org/2018/credentials/v1" ],
            "type" : [ "VerifiablePresentation" ],
            "holder" : "$HOLDER_DID",
            "proof" : {
                "type" : "Ed25519Signature2018",
                "created" : "2024-03-06T17:01:16Z",
                "domain" : "https://oid4vci.issuer.com/",
                "challenge" : "c_nonce value",
                "proofPurpose" : "authentication",
                "verificationMethod" : "did:key:z6MkkXhRtqiRsPK66xWM6mfZTaD1ApRar5gTwXn1S5qbyEbp#z6MkkXhRtqiRsPK66xWM6mfZTaD1ApRar5gTwXn1S5qbyEbp",
                "jws" : "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..HTDIjOyX-ri5F55_ut5d1nMDlKesazJjKYAqe7fRn3ohXGOOJ-WhrzGgHKPjUm_1hQEpnoSgbX6S7-so4fTVBg"
            }
        }"""

    const val JSON_WEB_SIGNATURE_PROOF_HOLDER = "did:key:z6MkhEnCvPLCFZWmjixzZJwhseroZpKDDMUefLjN8ifwq43z"
    const val JSON_WEB_SIGNATURE_PROOF = """
        {
          "@context" : [ "https://www.w3.org/2018/credentials/v1", "https://w3id.org/security/suites/jws-2020/v1" ],
          "type" : [ "VerifiablePresentation" ],
          "holder" : "did:key:z6MkhEnCvPLCFZWmjixzZJwhseroZpKDDMUefLjN8ifwq43z",
          "proof" : {
            "type" : "JsonWebSignature2020",
            "created" : "2024-06-25T10:06:37Z",
            "domain" : "https://oid4vci.issuer.com/",
            "challenge" : "c_nonce value",
            "proofPurpose" : "authentication",
            "verificationMethod" : "did:key:z6MkhEnCvPLCFZWmjixzZJwhseroZpKDDMUefLjN8ifwq43z#z6MkhEnCvPLCFZWmjixzZJwhseroZpKDDMUefLjN8ifwq43z",
            "jws" : "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..NXX6s5O0hkkZYY4IeyWqkYRlUsMxYefzVw4xZ6AQ6iWjaKrXiMHZ_pT9doSnWoyMUgp5b1SDdIpUU-ZMLPkIDw"
          }
        }
    """

    fun createIssuance(
        succesResultUrl: String = "",
        failResultUrl: String = "",
    ): IssuanceInitResponse {
        val req = IssuanceInitRequest(
            profileId = UUID.randomUUID().toString(),
            credentialData = jacksonObjectMapper().readTree(CRED_DATA),
            issuanceTimestamp = Instant.now(),
            holderDID = HOLDER_DID,
            invitationURL = null,
            successURL = URI.create(succesResultUrl),
            failureURL = URI.create(failResultUrl)
        )
        val res = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(req)
            .post("/api/v2/issuance/session")
            .then()
            .statusCode(200)
            .extract()
            .body().asString()

        return jacksonObjectMapper().readValue(res, IssuanceInitResponse::class.java)
    }
}
