package xfsc.not.api.oid4vci.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.xfsc.not.api.oid4vci.model.*
import eu.xfsc.not.testutil.assertEqualJsonTree
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestOid4vciMd {

    @Test
    fun `test Claims MD marshalling`() {
        val claimData = """
            {
                "given_names": [{
                    "display": [
                        {
                            "name": "Given Name",
                            "locale": "en-US"
                        },
                        {
                            "name": "Vorname",
                            "locale": "de-DE"
                        }
                    ]
                }],
                "family_name": {
                    "display": [
                        {
                            "name": "Surname",
                            "locale": "en-US"
                        },
                        {
                            "name": "Nachname",
                            "locale": "de-DE"
                        }
                    ]
                }
            }
        """.trimIndent()

        val om = jacksonObjectMapper()
        val ct: ClaimTree = om.readValue(claimData)
        Assertions.assertEquals(setOf("family_name"), ct.objTree.keys)
        Assertions.assertEquals("Surname", (ct.objTree["family_name"] as ClaimLeaf).display[0].name)
        Assertions.assertEquals(setOf("given_names"), ct.arrTree.keys)
        Assertions.assertEquals("Vorname", (ct.arrTree["given_names"]?.get(0) as ClaimLeaf).display[1].name)

        val claimData2 = om.writeValueAsString(ct)
        assertEqualJsonTree(claimData, claimData2, ignoreExtraFields = true)
    }

    @Test
    fun `test ReferencedAuthorizationDetails`() {
        val refAuthData = """
            {
                "type": "openid_credential",
                "credential_identifiers": ["mycred-1"],
                "credential_configuration_id": "mycred"
            }
        """.trimIndent()

        val om = jacksonObjectMapper()
        val ad: AuthorizationDetails = om.readValue(refAuthData)
        Assertions.assertInstanceOf(ReferencedAuthorizationDetails::class.java, ad)
    }

    @Test
    fun `test SpecifiedAuthorizationDetails`() {
        val refAuthData = """
            {
                "type": "openid_credential",
                "credential_identifiers": ["mycred-1"],
                "format": "vc+sd-jwt",
                "vct": "SD_JWT_VC_example_in_OpenID4VCI"
            }
        """.trimIndent()

        val om = jacksonObjectMapper()
        val ad: AuthorizationDetails = om.readValue(refAuthData)
        Assertions.assertInstanceOf(SpecifyingAuthorizationDetails::class.java, ad)
    }

}
