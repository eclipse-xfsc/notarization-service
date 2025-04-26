package eu.xfsc.not.ssi_issuance2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import eu.gaiax.notarization.api.issuance.IssueCredentialRequest
import eu.gaiax.notarization.api.issuance.ProofVerificationResult
import eu.gaiax.notarization.api.issuance.VerifyProofRequest
import eu.xfsc.not.api.oid4vci.model.CwtProof
import eu.xfsc.not.api.oid4vci.model.JwtProof
import eu.xfsc.not.api.oid4vci.model.LdpVpProof
import eu.xfsc.not.api.oid4vci.model.Proof
import eu.xfsc.not.ssi_issuance2.domain.ProfileProvider
import eu.xfsc.not.ssi_issuance2.mock.*
import io.quarkus.test.InjectMock
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.time.Instant
import java.util.*
import java.util.stream.Stream

private val log = KotlinLogging.logger {}

@QuarkusTest
@QuarkusTestResource(AcapyServiceMock::class)
@QuarkusTestResource(RevocationServiceMock::class)
@QuarkusTestResource(RequestProcessingMockImp::class)
class OidIssuanceResourceTest {

    @RequestProcessingMock
    lateinit var requestProcessingMock: WireMockServer

    @Inject
    lateinit var mapper: ObjectMapper

    @InjectMock
    lateinit var profileMock: ProfileProvider



    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun testVerifyProof() {
        ProfileMock.setUpFetchJsonLDProfile(profileMock)

        val succesResultPath = "/succ/${UUID.randomUUID()}"
        val failResultPath = "/fail/${UUID.randomUUID()}"

        val issuance = TestHelper.createIssuance(
            "${requestProcessingMock.baseUrl()}${succesResultPath}",
            "${requestProcessingMock.baseUrl()}${failResultPath}",
        )

        var token = issuance.cancelUrl.path?.substring(issuance.cancelUrl.path.toString().lastIndexOf("/") + 1)
        val proof = LdpVpProof(mapper.readTree(TestHelper.PROOF) as ObjectNode)

        val verifyRequest = VerifyProofRequest(
            "foo",
            TestHelper.PROOF_CHALLENGE,
            TestHelper.PROOF_DOMAIN,
            proof
        )
        given()
            .contentType(ContentType.JSON)
            .body(verifyRequest)
            .post("/api/v2/oid-issuance/${token}/verify-proof")
            .then()
            .statusCode(200)
            .extract()
            .body()//.asPrettyString()
            .jsonPath().getString("proof_pub_key").isNotEmpty()

    }
    companion object{
        @JvmStatic
        private fun notSupportedProofTypes(): Stream<Arguments> {

            return Stream.of(
                Arguments.of(
                    CwtProof(""),
                    JwtProof(""),
                )
            )
        }
    }

    @ParameterizedTest
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    @MethodSource("notSupportedProofTypes")
    fun testVerifyWrongProof(proof: Proof) {
        ProfileMock.setUpFetchJsonLDProfile(profileMock)

        val issuance = TestHelper.createIssuance()
        var token = issuance.cancelUrl.path?.substring(issuance.cancelUrl.path.toString().lastIndexOf("/") + 1)

        val verifyRequest = VerifyProofRequest(
            "foo",
            TestHelper.PROOF_CHALLENGE,
            TestHelper.PROOF_DOMAIN,
            proof
        )
        val resp = given()
            .contentType(ContentType.JSON)
            .body(verifyRequest)
            .post("/api/v2/oid-issuance/${token}/verify-proof")
            .then()
            .statusCode(400)
            .extract()
            .body()
            .jsonPath()

        assertThat(resp.getString("result"), `is` (ProofVerificationResult.INVALID_PROOF_TYPE.name))

    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun getCredTest() {
        ProfileMock.setUpFetchJsonLDProfile(profileMock)
        ProfileMock.setUpFetchDids(profileMock)

        val succesResultPath = "/succ/${UUID.randomUUID()}"
        val failResultPath = "/fail/${UUID.randomUUID()}"

        val issuance = TestHelper.createIssuance(
            "${requestProcessingMock.baseUrl()}${succesResultPath}",
            "${requestProcessingMock.baseUrl()}${failResultPath}",
        )

        val token = issuance.cancelUrl.path?.substring(issuance.cancelUrl.path.toString().lastIndexOf("/") + 1)

        val req = IssueCredentialRequest(
            profile = "profileId",
            subjectPubKey = TextNode.valueOf(TestHelper.HOLDER_DID),
        )

        val resp = given()
            .contentType(ContentType.JSON)
            .body(req)
            .post("/api/v2/oid-issuance/${token}/issue-credential")
            .then()
            .statusCode(200)
            .extract()
            .body()

        log.debug { "Created Credential : ${resp.asPrettyString()}" }

        val res = resp.jsonPath()

        assertThat("holder is missing in subject", res.getString("credential.credentialSubject.id"), `is`(TestHelper.HOLDER_DID))
        assertThat("proof is missing: ",res.getString("credential.proof.type"), notNullValue())
        assertThat("credStatus is missing: ",res.getString("credential.credentialStatus"), notNullValue())
        assertDoesNotThrow { Instant.parse(res.getString("credential.issuanceDate"))  }

        requestProcessingMock.verify(
            WireMock.postRequestedFor(
                WireMock.urlMatching("${succesResultPath}\\/?")
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun getErrorForUnsupportedCredType() {

        ProfileMock.setUpFetchAnonCredProfile(profileMock)
        ProfileMock.setUpFetchDids(profileMock)

        val succesResultPath = "/succ/${UUID.randomUUID()}"
        val failResultPath = "/fail/${UUID.randomUUID()}"

        val issuance = TestHelper.createIssuance(
            "${requestProcessingMock.baseUrl()}${succesResultPath}",
            "${requestProcessingMock.baseUrl()}${failResultPath}",
        )

        val token = issuance.cancelUrl.path?.substring(issuance.cancelUrl.path.toString().lastIndexOf("/") + 1)

        val req = IssueCredentialRequest(
                profile = "profileId",
                subjectPubKey = TextNode.valueOf(TestHelper.HOLDER_DID),
            )


        val res = given()
            .contentType(ContentType.JSON)
            .body(req)
            .post("/api/v2/oid-issuance/${token}/issue-credential")
            .then()
            .statusCode(400)
            .extract()
            .body().jsonPath()


        assertThat(res.getString("result"), `is`("UNKNOWN_ERROR"))

        requestProcessingMock.verify(
            WireMock.postRequestedFor(
                WireMock.urlMatching("${failResultPath}\\/?")
            )
        )
    }
}
