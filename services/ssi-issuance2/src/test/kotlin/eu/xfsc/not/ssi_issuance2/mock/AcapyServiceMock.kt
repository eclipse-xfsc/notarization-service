package eu.xfsc.not.ssi_issuance2.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class AcapyServiceMock : QuarkusTestResourceLifecycleManager {

    private val VALIDATION_RESULT = """{
    "verified": true,
    "presentation_result": {
        "verified": true,
        "document": {
            "@context": [
                "https://www.w3.org/2018/credentials/v1"
            ],
            "type": [
                "VerifiablePresentation"
            ],
            "holder": "${TestHelper.HOLDER_DID}",
            "proof": {
                "type": "Ed25519Signature2018",
                "proofPurpose": "authentication",
                "verificationMethod": "did:key:z6MkkXhRtqiRsPK66xWM6mfZTaD1ApRar5gTwXn1S5qbyEbp#z6MkkXhRtqiRsPK66xWM6mfZTaD1ApRar5gTwXn1S5qbyEbp",
                "created": "2024-03-06T17:01:16Z",
                "domain": "https://oid4vci.issuer.com/",
                "challenge": "c_nonce value",
                "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..HTDIjOyX-ri5F55_ut5d1nMDlKesazJjKYAqe7fRn3ohXGOOJ-WhrzGgHKPjUm_1hQEpnoSgbX6S7-so4fTVBg"
            }
        }
    }

    }""".trimIndent()

    private val DID_CREATE_RESP =
        """ {
        "result": {
            "did": "did:key:z6MksBRnLRxGapWWSVRAnT6W5UyUrxdVqnjQH3GCUdBvy357",
            "verkey": "DjAjkBhqFH23KzaU6t8fEPRV3PMeRuV3b2MGeMDv3pHj",
            "posture": "wallet_only",
            "key_type": "ed25519",
            "method": "key"
        }
    } """


    private val DID_RESP = """ {
          "results": [
            {
              "did": "did:key:z6MkvgBYjNXgksYEz2Ya5QHVu6YbkVXJrxTeXEZwffEAsHJo",
              "verkey": "HDvW98HFRL3msXhsPqKf3zzbvvFTT5DHqDf1qPG9x4XR",
              "posture": "wallet_only",
              "key_type": "ed25519",
              "method": "key"
            }
          ]
        }
    """

    private val ISSUE_RESP = """{
        "verifiableCredential": {
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://w3id.org/citizenship/v1",
            "https://w3id.org/vc/status-list/2021/v1",
            "https://w3id.org/security/suites/ed25519-2020/v1"
        ],
        "id": "urn:uuid:fe6fe639-267e-4b49-a54e-e07fb42bed48",
        "type": [
            "VerifiableCredential",
            "PermanentResidentCard"
        ],
        "issuer": "did:key:z6MknJEPQ8KVnhQ9xLQSBKpXLAAMieLmmEzyj2pUE3ahGs6k",
        "issuanceDate": "2024-03-19T15:12:11.731297Z",
        "credentialSubject": {
            "type": "PermanentResident",
            "id": "did:key:z6MkkXhRtqiRsPK66xWM6mfZTaD1ApRar5gTwXn1S5qbyEbp",
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
        },
        "credentialStatus": {
            "id": "https://example.com/credentials/status/3#94567",
            "statusListCredential": "https://example.com/credentials/status/3",
            "statusListIndex": "94567",
            "statusPurpose": "revocation",
            "type": "StatusList2021Entry"
        },
        "proof": {
            "type": "Ed25519Signature2020",
            "proofPurpose": "assertionMethod",
            "verificationMethod": "did:key:z6MknJEPQ8KVnhQ9xLQSBKpXLAAMieLmmEzyj2pUE3ahGs6k#z6MknJEPQ8KVnhQ9xLQSBKpXLAAMieLmmEzyj2pUE3ahGs6k",
            "created": "2024-03-19T15:12:11+00:00",
            "proofValue": "z2ZdQA9pr8Z7ib9TT3nmJMcxyKR5NhmKM2jDDfb85E6U3ujfp2EjuavYigsoKiU2crydNNYrwH1zQ4dv4P5xubdmN"
        }
    }
}"""


    private var mockServer: WireMockServer = WireMockServer(
        WireMockConfiguration.options()
            .dynamicPort()
    )

    override fun start(): MutableMap<String, String> {
        mockServer.resetAll()
        mockServer.apply {
            start()
            stubFor(
                WireMock.get(
                    WireMock.urlMatching("/wallet/did?.*")
                )
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(DID_RESP)
                    )
            )
            stubFor(
                WireMock.post(
                    WireMock.urlMatching("/wallet/did/create")
                ).withRequestBody(
                    matchingJsonPath("$.method", WireMock.equalTo("key"))
                ).withRequestBody(
                    matchingJsonPath("$.options.key_type", WireMock.or(
                            WireMock.equalTo("ed25519"),
                            WireMock.equalTo("bls12381g2"),
                        )
                    )
                )
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(DID_CREATE_RESP)
                    )
            )
            stubFor(
                WireMock.post(
                    WireMock.urlMatching("/vc/credentials/issue")
                )
                    .withRequestBody(matchingJsonPath("$.credential.@context"))
                    .withRequestBody(matchingJsonPath("$.credential['@context'][?(@ =~ /.+w3id.+status-list.+/)]"))
                    .withRequestBody(matchingJsonPath("$.credential.credentialSubject"))
                    .withRequestBody(matchingJsonPath("$.credential.id"))
                    .withRequestBody(matchingJsonPath("$.credential.issuanceDate"))
                    .withRequestBody(matchingJsonPath("$.credential.issuer"))
                    .withRequestBody(matchingJsonPath("$.credential.type"))
                    .withRequestBody(matchingJsonPath("$.options.type",
                            WireMock.or(
                                WireMock.equalTo("Ed25519Signature2018"),
                                WireMock.equalTo("Ed25519Signature2020"),
                                WireMock.equalTo("BbsBlsSignature2020"),
                                WireMock.equalTo("BbsBlsSignatureProof2020"),
                            )
                        )
                    )
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(ISSUE_RESP)
                    )
            )
            stubFor(
                WireMock.post(
                    WireMock.urlMatching("/vc/presentations/verify")
                )
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(VALIDATION_RESULT)
                    )
            )
        }

        return mutableMapOf(("quarkus.rest-client.acapy_json.url" to mockServer.baseUrl()))
    }

    override fun stop() {
        mockServer.stop()
    }
}

