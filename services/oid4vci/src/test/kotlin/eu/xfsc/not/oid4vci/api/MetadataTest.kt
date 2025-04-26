package eu.xfsc.not.oid4vci.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.xfsc.not.oid4vci.Oid4vciConfig
import eu.xfsc.not.oid4vci.ProfileProvider
import eu.xfsc.not.oid4vci.WellKnownConfigApiImpl
import eu.xfsc.not.oid4vci.api.ProfileMock.setupAnonCredProfile
import eu.xfsc.not.oid4vci.api.ProfileMock.setupDefectiveProfile
import eu.xfsc.not.oid4vci.api.ProfileMock.setupDummyProfile
import eu.xfsc.not.oid4vci.api.ProfileMock.setupEmptyProfile
import eu.xfsc.not.testutil.assertEqualJsonTree
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry


@QuarkusTest
class MetadataTest {

    @Inject
    lateinit var mdService: WellKnownConfigApiImpl
    @Inject
    lateinit var oidConf: Oid4vciConfig
    @InjectMock
    lateinit var profileMock: ProfileProvider

    val issuerDisplayRef: String get() {
        return oidConf.issuerDisplay().get()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test build Dummy OID4VCI Metadata`() {
        setupDummyProfile(profileMock)

        val om = jacksonObjectMapper()

        val oauthConf = mdService.oauthConfig()
        val vciConf = mdService.oidVciConfig()
        val oauthConfActual = om.writeValueAsString(oauthConf)
        val vciConfActual = om.writeValueAsString(vciConf)

        assertEqualJsonTree("""
            {
                "issuer": "https://localhost:8088/",
                "token_endpoint": "https://localhost:8088/oid4vci/token",
                "jwks_uri": "https://localhost:8088/oauth/jwks",
                "response_types_supported": [],
                "grant_types_supported": ["urn:ietf:params:oauth:grant-type:pre-authorized_code"],
                "pre-authorized_grant_anonymous_access_supported": true
            }
        """.trimIndent(), oauthConfActual)

        assertEqualJsonTree("""
            {
                "credential_issuer": "https://localhost:8088/",
                "credential_endpoint": "https://localhost:8088/oid4vci/credential",
                "credential_response_encryption": {
                    "alg_values_supported": ["RSA1_5","RSA-OAEP","RSA-OAEP-256","ECDH-ES","ECDH-ES+A128KW","ECDH-ES+A192KW","ECDH-ES+A256KW"],
                    "enc_values_supported": ["A128CBC-HS256","A192CBC-HS384","A256CBC-HS512","A128GCM","A192GCM","A256GCM"],
                    "encryption_required": false
                },
                "credential_identifiers_supported": true,
                "credential_configurations_supported": {
                    "foo": {
                        "credential_definition": {
                            "@context": ["https://www.w3.org/2018/credentials/v1","https://www.w3.org/2018/credentials/examples/v1"],
                            "type": ["VerifiableCredential","AlumniCredential"]
                        },
                        "format": "ldp_vc",
                        "credential_signing_alg_values_supported": ["Ed25519Signature2018","Ed25519Signature2020","JsonWebSignature2020","BbsBlsSignature2020"],
                        "proof_types_supported": {
                            "ldp_vp": {
                                "proof_signing_alg_values_supported": ["Ed25519Signature2018","Ed25519Signature2020","JsonWebSignature2020","BbsBlsSignatureProof2020"]
                            }
                        }
                    }
                },
                "display": $issuerDisplayRef
            }
        """.trimIndent(), vciConfActual)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test build Empty OID4VCI Metadata`() {
        setupEmptyProfile(profileMock)

        val om = jacksonObjectMapper()

        val oauthConf = mdService.oauthConfig()
        val vciConf = mdService.oidVciConfig()
        val oauthConfActual = om.writeValueAsString(oauthConf)
        val vciConfActual = om.writeValueAsString(vciConf)

        assertEqualJsonTree("""
            {
                "issuer": "https://localhost:8088/",
                "token_endpoint": "https://localhost:8088/oid4vci/token",
                "jwks_uri": "https://localhost:8088/oauth/jwks",
                "response_types_supported": [],
                "grant_types_supported": ["urn:ietf:params:oauth:grant-type:pre-authorized_code"],
                "pre-authorized_grant_anonymous_access_supported": true
            }
        """.trimIndent(), oauthConfActual)

        assertEqualJsonTree("""
            {
                "credential_issuer": "https://localhost:8088/",
                "credential_endpoint": "https://localhost:8088/oid4vci/credential",
                "credential_response_encryption": {
                    "alg_values_supported": ["RSA1_5","RSA-OAEP","RSA-OAEP-256","ECDH-ES","ECDH-ES+A128KW","ECDH-ES+A192KW","ECDH-ES+A256KW"],
                    "enc_values_supported": ["A128CBC-HS256","A192CBC-HS384","A256CBC-HS512","A128GCM","A192GCM","A256GCM"],
                    "encryption_required": false
                },
                "credential_identifiers_supported": true,
                "credential_configurations_supported": {

                },
                "display": $issuerDisplayRef
            }
        """.trimIndent(), vciConfActual)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test build AnonCred OID4VCI Metadata`() {
        setupAnonCredProfile(profileMock)

        val om = jacksonObjectMapper()

        val oauthConf = mdService.oauthConfig()
        val vciConf = mdService.oidVciConfig()
        val oauthConfActual = om.writeValueAsString(oauthConf)
        val vciConfActual = om.writeValueAsString(vciConf)

        assertEqualJsonTree("""
            {
                "issuer": "https://localhost:8088/",
                "token_endpoint": "https://localhost:8088/oid4vci/token",
                "jwks_uri": "https://localhost:8088/oauth/jwks",
                "response_types_supported": [],
                "grant_types_supported": ["urn:ietf:params:oauth:grant-type:pre-authorized_code"],
                "pre-authorized_grant_anonymous_access_supported": true
            }
        """.trimIndent(), oauthConfActual)

        assertEqualJsonTree("""
            {
                "credential_issuer": "https://localhost:8088/",
                "credential_endpoint": "https://localhost:8088/oid4vci/credential",
                "credential_response_encryption": {
                    "alg_values_supported": ["RSA1_5","RSA-OAEP","RSA-OAEP-256","ECDH-ES","ECDH-ES+A128KW","ECDH-ES+A192KW","ECDH-ES+A256KW"],
                    "enc_values_supported": ["A128CBC-HS256","A192CBC-HS384","A256CBC-HS512","A128GCM","A192GCM","A256GCM"],
                    "encryption_required": false
                },
                "credential_identifiers_supported": true,
                "credential_configurations_supported": {

                },
                "display": $issuerDisplayRef
            }
        """.trimIndent(), vciConfActual)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `test build Defective OID4VCI Metadata`() {
        setupDefectiveProfile(profileMock)

        val om = jacksonObjectMapper()

        val oauthConf = mdService.oauthConfig()
        val vciConf = mdService.oidVciConfig()
        val oauthConfActual = om.writeValueAsString(oauthConf)
        val vciConfActual = om.writeValueAsString(vciConf)

        assertEqualJsonTree("""
            {
                "issuer": "https://localhost:8088/",
                "token_endpoint": "https://localhost:8088/oid4vci/token",
                "jwks_uri": "https://localhost:8088/oauth/jwks",
                "response_types_supported": [],
                "grant_types_supported": ["urn:ietf:params:oauth:grant-type:pre-authorized_code"],
                "pre-authorized_grant_anonymous_access_supported": true
            }
        """.trimIndent(), oauthConfActual)

        assertEqualJsonTree("""
            {
                "credential_issuer": "https://localhost:8088/",
                "credential_endpoint": "https://localhost:8088/oid4vci/credential",
                "credential_response_encryption": {
                    "alg_values_supported": ["RSA1_5","RSA-OAEP","RSA-OAEP-256","ECDH-ES","ECDH-ES+A128KW","ECDH-ES+A192KW","ECDH-ES+A256KW"],
                    "enc_values_supported": ["A128CBC-HS256","A192CBC-HS384","A256CBC-HS512","A128GCM","A192GCM","A256GCM"],
                    "encryption_required": false
                },
                "credential_identifiers_supported": true,
                "credential_configurations_supported": {
                    "foo": {
                        "credential_definition": {
                            "@context": [],
                            "type": ["VerifiableCredential","AlumniCredential"]
                        },
                        "format": "ldp_vc",
                        "credential_signing_alg_values_supported": ["Ed25519Signature2018","Ed25519Signature2020","JsonWebSignature2020","BbsBlsSignature2020"],
                        "proof_types_supported": {
                            "ldp_vp": {
                                "proof_signing_alg_values_supported": ["Ed25519Signature2018","Ed25519Signature2020","JsonWebSignature2020","BbsBlsSignatureProof2020"]
                            }
                        }
                    }
                },
                "display": $issuerDisplayRef
            }
        """.trimIndent(), vciConfActual)
    }

}
