package eu.xfsc.not.oid4vci.api

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.xfsc.not.api.oid4vci.model.*
import eu.xfsc.not.testutil.assertEqualJsonTree
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junitpioneer.jupiter.ReportEntry

class Oidc4VciModelsTest {

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test read untyped JWT-Proof CredentialRequest`() {
        val om = jacksonObjectMapper()
        val reqDataStr1 = """
            {
               "format": "ldp_vcc",
               "proof": {
                  "proof_type": "jwt",
                  "jwt": "eyJraWQiOiJkaWQ6ZXhhbXBsZ...KPxgihac0aW9EkL1nOzM"
               }
            }
        """.trimIndent()

        assertThrows<InvalidFormatException> { om.readValue<CredentialRequest>(reqDataStr1) }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test read W3C VC-LD JWT-Proof CredentialRequest`() {
        val om = jacksonObjectMapper()
        val reqDataStr1 = """
            {
               "format": "ldp_vc",
               "credential_definition": {
                  "@context": [
                     "https://www.w3.org/2018/credentials/v1",
                     "https://www.w3.org/2018/credentials/examples/v1"
                  ],
                  "type": [
                     "VerifiableCredential",
                     "UniversityDegreeCredential"
                  ],
                  "credentialSubject": {
                     "degree": {
                        "type": {}
                     }
                  }
               },
               "proof": {
                  "proof_type": "jwt",
                  "jwt": "eyJraWQiOiJkaWQ6ZXhhbXBsZ...KPxgihac0aW9EkL1nOzM"
               }
            }
        """.trimIndent()

        val reqData1: CredentialRequest = om.readValue(reqDataStr1)
        val reqDataStr2 = om.writeValueAsString(reqData1)
        val reqData2: CredentialRequest = om.readValue(reqDataStr2)
        assertEqualJsonTree(reqDataStr1, reqDataStr2)

        assertInstanceOf(SpecifyingCredentialRequest::class.java, reqData2).also {
            assertEquals(CredentialFormatEnum.VC_LDP_JSON_LD, it.ext.format)
            assertInstanceOf(W3cCredentialRequestExtension::class.java, it.ext).also {
                assertInstanceOf(ObjectNode::class.java, it.credentialDefinition).also {
                    assertTrue(it.has("@context"))
                    assertTrue(it.has("type"))
                    assertTrue(it.has("credentialSubject"))
                }
            }
            assertInstanceOf(JwtProof::class.java, it.proof).also {
                assertEquals(ProofTypeEnum.JWT, it.proofType)
                assertTrue(it.jwt.startsWith("eyJraWQiOiJkaWQ6ZXhhbXBsZ"))
            }
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test read W3C VC-LD LD-Proof CredentialRequest`() {
        val om = jacksonObjectMapper()
        val reqDataStr1 = """
            {
               "format": "ldp_vc",
               "credential_definition": {
                  "@context": [
                     "https://www.w3.org/2018/credentials/v1",
                     "https://www.w3.org/2018/credentials/examples/v1"
                  ],
                  "type": [
                     "VerifiableCredential",
                     "UniversityDegreeCredential"
                  ],
                  "credentialSubject": {
                     "degree": {
                        "type": {}
                     }
                  }
               },
               "proof": {
                  "proof_type": "ldp_vp",
                  "ldp_vp": {
                     "@context": [
                        "https://www.w3.org/ns/credentials/v2",
                        "https://www.w3.org/ns/credentials/examples/v2"
                     ],
                     "type": [
                        "VerifiablePresentation"
                     ],
                     "holder": "did:key:z6MkvrFpBNCoYewiaeBLgjUDvLxUtnK5R6mqh5XPvLsrPsro",
                     "proof": [
                        {
                           "type": "DataIntegrityProof",
                           "cryptosuite": "eddsa-2022",
                           "proofPurpose": "authentication",
                           "verificationMethod": "did:key:z6MkvrFpBNCoYewiaeBLgjUDvLxUtnK5R6mqh5XPvLsrPsro#z6MkvrFpBNCoYewiaeBLgjUDvLxUtnK5R6mqh5XPvLsrPsro",
                           "created": "2023-03-01T14:56:29.280619Z",
                           "challenge": "82d4cb36-11f6-4273-b9c6-df1ac0ff17e9",
                           "domain": "did:web:audience.company.com",
                           "proofValue": "z5hrbHzZiqXHNpLq6i7zePEUcUzEbZKmWfNQzXcUXUrqF7bykQ7ACiWFyZdT2HcptF1zd1t7NhfQSdqrbPEjZceg7"
                        }
                     ]
                  }
               }
            }
        """.trimIndent()

        val reqData1: CredentialRequest = om.readValue(reqDataStr1)
        val reqDataStr2 = om.writeValueAsString(reqData1)
        val reqData2: CredentialRequest = om.readValue(reqDataStr2)
        assertEqualJsonTree(reqDataStr1, reqDataStr2)

        assertInstanceOf(SpecifyingCredentialRequest::class.java, reqData2).also {
            assertEquals(CredentialFormatEnum.VC_LDP_JSON_LD, it.ext.format)
            assertInstanceOf(W3cCredentialRequestExtension::class.java, it.ext).also {
                assertInstanceOf(ObjectNode::class.java, it.credentialDefinition).also {
                    assertTrue(it.has("@context"))
                    assertTrue(it.has("type"))
                    assertTrue(it.has("credentialSubject"))
                }
            }
            assertInstanceOf(LdpVpProof::class.java, it.proof).also {
                assertEquals(ProofTypeEnum.LDP_VP, it.proofType)
                assertTrue(it.ldpVp.has("@context"))
                assertTrue(it.ldpVp.has("type"))
                assertTrue(it.ldpVp.has("holder"))
                assertTrue(it.ldpVp.has("proof"))
            }
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test read W3C VC-LD Sync CredentialResponse`() {
        val om = jacksonObjectMapper()
        val dataStr1 = """
            {
                "credential": {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1",
                        "https://www.w3.org/2018/credentials/examples/v1"
                    ],
                    "id": "http://example.edu/credentials/3732",
                    "type": [
                        "VerifiableCredential",
                        "UniversityDegreeCredential"
                    ],
                    "issuer": "https://example.edu/issuers/565049",
                    "issuanceDate": "2010-01-01T00:00:00Z",
                    "credentialSubject": {
                        "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                        "degree": {
                            "type": "BachelorDegree",
                            "name": "Bachelor of Science and Arts"
                        }
                    },
                    "proof": {
                        "type": "Ed25519Signature2020",
                        "created": "2022-02-25T14:58:43Z",
                        "verificationMethod": "https://example.edu/issuers/565049#key-1",
                        "proofPurpose": "assertionMethod",
                        "proofValue": "zeEdUoM7m9cY8ZyTpey83yBKeBcmcvbyrEQzJ19rD2UXArU2U1jPGoEtrRvGYppdiK37GU4NBeoPakxpWhAvsVSt"
                    }
                },
                "c_nonce": "fGFF7UkhLa",
                "c_nonce_expires_in": 86400
            }
        """.trimIndent()

        val data1: CredentialResponse = om.readValue(dataStr1)
        val dataStr2 = om.writeValueAsString(data1)
        val data2: CredentialResponse = om.readValue(dataStr2)
        assertEqualJsonTree(dataStr1, dataStr2)

        assertInstanceOf(CredentialResponseSync::class.java, data2).also {
            assertInstanceOf(ObjectNode::class.java, it.credential).also {
            }
            assertEquals("fGFF7UkhLa", it.cNonce)
            assertEquals(86400, it.cNonceExpiresIn)
            assertNull(it.notificationId)
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test read W3C VC-LD Async CredentialResponse`() {
        val om = jacksonObjectMapper()
        val dataStr1 = """
            {
                "transaction_id": "2993jdjsjs29skjz",
                "c_nonce": "fGFF7UkhLa",
                "c_nonce_expires_in": 86400
            }
        """.trimIndent()

        val data1: CredentialResponse = om.readValue(dataStr1)
        val dataStr2 = om.writeValueAsString(data1)
        val data2: CredentialResponse = om.readValue(dataStr2)
        assertEqualJsonTree(dataStr1, dataStr2)

        assertInstanceOf(CredentialResponseAsync::class.java, data2).also {
            assertEquals("2993jdjsjs29skjz", it.transactionId)
            assertEquals("fGFF7UkhLa", it.cNonce)
            assertEquals(86400, it.cNonceExpiresIn)
            assertNull(it.notificationId)
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test read JWT JSON Metadata`() {
        val om = jacksonObjectMapper()
        val dataStr1 = """
            {
                "credential_issuer": "https://example.com",
                "credential_endpoint": "https://example.com/credentials",
                "credential_configurations_supported": {
                    "UniversityDegreeCredential": {
                        "format": "jwt_vc_json",
                        "scope": "UniversityDegree",
                        "cryptographic_binding_methods_supported": [
                            "did:example"
                        ],
                        "credential_signing_alg_values_supported": [
                            "ES256"
                        ],
                        "credential_definition":{
                            "type": [
                                "VerifiableCredential",
                                "UniversityDegreeCredential"
                            ],
                            "credentialSubject": {
                                "given_name": {
                                    "display": [
                                        {
                                            "name": "Given Name",
                                            "locale": "en-US"
                                        }
                                    ]
                                },
                                "family_name": {
                                    "display": [
                                        {
                                            "name": "Surname",
                                            "locale": "en-US"
                                        }
                                    ]
                                },
                                "degree": {},
                                "gpa": {
                                    "mandatory": true,
                                    "display": [
                                        {
                                            "name": "GPA"
                                        }
                                    ]
                                }
                            }
                        },
                        "proof_types_supported": {
                            "jwt": {
                                "proof_signing_alg_values_supported": [
                                    "ES256"
                                ]
                            }
                        },
                        "display": [
                            {
                                "name": "University Credential",
                                "locale": "en-US",
                                "logo": {
                                    "uri": "https://university.example.edu/public/logo.png",
                                    "alt_text": "a square logo of a university"
                                },
                                "background_color": "#12107c",
                                "text_color": "#FFFFFF"
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        val data1: CredentialIssuerMetadata = om.readValue(dataStr1)
        val dataStr2 = om.writeValueAsString(data1)
        val data2: CredentialIssuerMetadata = om.readValue(dataStr2)
//        assertEqualJsonTree(dataStr1, dataStr2)

        assertEquals("https://example.com", data2.credentialIssuer)
        assertEquals("https://example.com/credentials", data2.credentialEndpoint)
        assertInstanceOf(W3cJwtJsonCredentialConfigurationSupported::class.java, data2.credentialConfigurationsSupported["UniversityDegreeCredential"]).also {
            assertEquals(CredentialFormatEnum.VC_JWT_JSON.value, it.format)
            assertEquals("UniversityDegree", it.scope)
            assertIterableEquals(listOf("did:example"), it.cryptographicBindingMethodsSupported)
            assertIterableEquals(listOf("ES256"), it.credentialSigningAlgValuesSupported)

            val def = it.credentialDefinition
            assertIterableEquals(listOf("VerifiableCredential", "UniversityDegreeCredential"), def.type)
            def.credentialSubject["given_name"]?.also {
                assertFalse(it.mandatory)
                assertNull(it.valueType)
                assertTrue(it.display.size == 1)
            } ?: fail("Missing claim definition given_name")
            def.credentialSubject["degree"]?.also {
                assertFalse(it.mandatory)
                assertNull(it.valueType)
                assertTrue(it.display.isEmpty())
            } ?: fail("Missing claim definition degree")

            it.display[0].also {
                assertEquals("University Credential", it.name)
                assertEquals("en-US", it.locale)
                it.logo?.also {
                    assertEquals("https://university.example.edu/public/logo.png", it.uri)
                    assertEquals("a square logo of a university", it.altText)
                } ?: fail("Missing credential logo definition")
            }
        }
    }

}
