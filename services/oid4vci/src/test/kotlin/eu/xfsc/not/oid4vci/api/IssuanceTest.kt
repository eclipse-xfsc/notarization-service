package eu.xfsc.not.oid4vci.api

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.common.Urls
import eu.gaiax.notarization.api.issuance.IssueCredentialSuccess
import eu.gaiax.notarization.api.issuance.VerifyProofSuccess
import eu.xfsc.not.api.oid4vci.Oid4VciOfferApi
import eu.xfsc.not.api.oid4vci.model.*
import eu.xfsc.not.api.oid4vci.model.TokenTypes.Bearer
import eu.xfsc.not.oid4vci.IssuanceClientBuilder
import eu.xfsc.not.oid4vci.OidIssuanceApiClient
import eu.xfsc.not.oid4vci.Oidc4VciImpl
import eu.xfsc.not.oid4vci.ProfileProvider
import eu.xfsc.not.oid4vci.api.ProfileMock.setupDummyProfile
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*

@QuarkusTest
class IssuanceTest {

    val profile = "foo"
    val keyObj: ObjectNode = jacksonObjectMapper().readValue("""{"jwk":"foo"}""")
    val credObj: ObjectNode = jacksonObjectMapper().readValue("""{"@context":[], "type":"ldp_vc"}""")

    lateinit var issApi: OidIssuanceApiClient

    @InjectMock
    lateinit var profileMock: ProfileProvider
    @InjectMock
    lateinit var issApiProv: IssuanceClientBuilder

    @Inject
    lateinit var offerApi: Oid4VciOfferApi
    @Inject
    lateinit var oidApi: Oidc4VciImpl

    @BeforeEach
    fun setupIssClient() {
        issApi = mock()

        whenever(issApiProv.buildClient(anyString())).then {
            issApi
        }

        whenever(issApi.verifyProof(isA())).then {
            VerifyProofSuccess(keyObj)
        }

        whenever(issApi.issueCredential(isA())).then {
            IssueCredentialSuccess(credential = credObj)
        }
    }

    @AfterEach
    fun checkMockInvocations() {
        verify(issApi).issueCredential(argThat {
            this.subjectPubKey === keyObj && this.profile == "foo"
        })
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun `happy-path`() {
        setupDummyProfile(profileMock)

        // process offer
        val offerUri = offerApi.createCredentialOffer(listOf(profile), "iss-session", "http://callback.url/")
        val offerValue = Urls.splitQueryFromUrl(offerUri)["credential_offer"]
        val offer: CredentialOffer = offerValue?.firstValue()?.let { jacksonObjectMapper().readValue(it) }!!
        Assertions.assertEquals(listOf(profile), offer.credentialConfigurationIds)
        val code = offer.grants?.preAuthCodeGrantOffer?.preAuthorizedCode!!

        // get access token
        val tokenRes = oidApi.oidToken(Oid4VciTokenRequest().also {
            it.grantType = GrantTypes.PreAuthCodeName
            it.preAuthorizedCode = code
        })
        Assertions.assertNotNull(tokenRes.cNonce)
        Assertions.assertNotNull(tokenRes.cNonceExpiresIn)
        Assertions.assertEquals(Bearer, tokenRes.oauthTokenResponse.tokenType)
        Assertions.assertNotNull(tokenRes.oauthTokenResponse.accessToken)
        Assertions.assertNotNull(tokenRes.oauthTokenResponse.expiresIn)
        Assertions.assertNull(tokenRes.oauthTokenResponse.refreshToken)
        Assertions.assertNull(tokenRes.oauthTokenResponse.scope)

        // issue credential
        Given {
            auth().oauth2(tokenRes.oauthTokenResponse.accessToken)
            accept("application/json")
            contentType("application/json")
            body(ReferencedCredentialRequest(profile).also {
                it.proof = LdpVpProof(
                    ldpVp = jacksonObjectMapper().readValue("{}")
                )
            })
        } When {
            post("/oid4vci/credential")
        } Then {
            statusCode(200)
            // verify response
            body("c_nonce", notNullValue())
            body("credential.type", equalTo("ldp_vc"))
        }
    }

}
