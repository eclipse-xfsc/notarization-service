/*
 *
 */
package eu.gaiax.notarization.request_processing

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BooleanNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import eu.gaiax.notarization.MockSsiIssuanceLifecycleManager
import eu.gaiax.notarization.SsiIssuanceWireMock
import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.gaiax.notarization.request_processing.domain.entity.Document
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.SsiIssuanceClient
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.json.JsonValue
import org.apache.commons.codec.binary.Hex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.time.OffsetDateTime
import java.time.Period
import java.util.*
import java.util.List
import java.util.Set
import java.util.function.Consumer
import java.util.function.Supplier

/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@QuarkusTestResource(MockSsiIssuanceLifecycleManager::class)
class DocumentInclusionTest {
    @SsiIssuanceWireMock
    lateinit var issuanceWireMock: WireMockServer

    @Inject
    lateinit var ssiIssuanceClient: SsiIssuanceClient

    @Inject
    lateinit var mapper: ObjectMapper
    @BeforeEach
    fun setup() {
        issuanceWireMock.resetRequests()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00038")
    @RunOnVertxContext
    fun givenAbsentTemplateThenDataIsUnchanged(asserter: UniAsserter) {
        val inputRequest = someRequest()
        val givenNullTemplate: String? = null
        asserter.assertThat ( {
                ssiIssuanceClient.issue(
                    inputRequest,
                    someProfileWithDocumentTemplate(givenNullTemplate)
                )
            },
             { result ->
                issuanceWireMock.verify(
                    WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                        .withRequestBody(
                            WireMock.matchingJsonPath(
                                "$.credentialData",
                                WireMock.equalToJson(inputRequest.data)
                            )
                        )
                )
            })
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00038")
    @RunOnVertxContext
    fun givenStaticTemplateThenDataIsCorrectMergedResult(asserter: UniAsserter) {
        val inputRequest = someRequest()
        val givenStaticTemplate: String =
            """
                { "${UUID.randomUUID()}": "${UUID.randomUUID()}" }
            """.trimIndent()
        asserter.assertThat( {
                ssiIssuanceClient.issue(
                    inputRequest,
                    someProfileWithDocumentTemplate(givenStaticTemplate)
                )
            },
            { result ->
                val expectedData = mergeJson(inputRequest.data!!, givenStaticTemplate)
                issuanceWireMock.verify(
                    WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                        .withRequestBody(
                            WireMock.matchingJsonPath(
                                "$.credentialData",
                                WireMock.equalToJson(expectedData)
                            )
                        )
                )
            })
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00038")
    @RunOnVertxContext
    fun givenExampleAip1TemplateThenRequestIsCorrect(asserter: UniAsserter) {
        val inputRequest = someRequest()
        val givenHash1 = byteArrayOf(1, 2, 3, 4)
        val givenHash2 = byteArrayOf(3, 4, 5, 6)
        val givenHash3 = byteArrayOf(6, 7, 8, 9)
        inputRequest.session!!.documents = Set.of( // These UUIDs are sorted to ensure document order is kept
            someDocument(UUID.fromString("25169b2b-a657-46d8-b3ce-429f1e895656"), givenHash1),
            someDocument(UUID.fromString("97fa3276-8e00-4814-8d16-13998798a8a8"), givenHash2),
            someDocument(UUID.fromString("d8925a3c-5e60-4cc0-bcfd-1cebe7d44dc9"), givenHash3)
        )
        val givenTemplate: String =
            """
                { "evidenceDocument": "<documents:{doc|<doc.sha3_256.base64>}; separator=", ">" }
            """.trimIndent()
            asserter.assertThat( {
                    ssiIssuanceClient.issue(
                        inputRequest,
                        someProfileWithDocumentTemplate(givenTemplate)
                    )
                }, { result ->
                    val expectedData = mergeJson(
                        inputRequest.data!!, String.format(
                            """
                                { "evidenceDocument": "%s, %s, %s" }
                            """.trimIndent(),
                            Base64.getEncoder().encodeToString(givenHash1),
                            Base64.getEncoder().encodeToString(givenHash2),
                            Base64.getEncoder().encodeToString(givenHash3)
                        )
                    )
                    issuanceWireMock.verify(
                        WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                            .withRequestBody(
                                WireMock.matchingJsonPath(
                                    "$.credentialData",
                                    WireMock.equalToJson(expectedData)
                                )
                            )
                    )
                })
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00038")
    @RunOnVertxContext
    fun givenExampleAip2TemplateThenRequestIsCorrect(asserter: UniAsserter) {
        val inputRequest: NotarizationRequest = someRequest()
        val givenHash1 = byteArrayOf(1, 2, 3, 4)
        val givenHash2 = byteArrayOf(3, 4, 5, 6)
        val givenHash3 = byteArrayOf(6, 7, 8, 9)
        inputRequest.session!!.documents = Set.of( // These UUIDs are sorted to ensure document order is kept
            someDocument(UUID.fromString("25169b2b-a657-46d8-b3ce-429f1e895656"), givenHash1),
            someDocument(UUID.fromString("97fa3276-8e00-4814-8d16-13998798a8a8"), givenHash2),
            someDocument(UUID.fromString("d8925a3c-5e60-4cc0-bcfd-1cebe7d44dc9"), givenHash3)
        )
        val givenTemplate: String =
            """
                { "credentialSubject": { "evidenceDocument": [ <documents:{doc|"<doc.sha3_256.base64>"}; separator=" ,">  ] } }
            """.trimIndent()
            asserter.assertThat(
                {
                    ssiIssuanceClient.issue(
                        inputRequest,
                        someProfileWithDocumentTemplate(AipVersion.V2_0, givenTemplate)
                    )
                },
                { result ->
                    val expectedSubject = mergeJson(
                        inputRequest.data!!, kotlin.String.format(
                            """
                                { "id": "%s", "evidenceDocument": ["%s", "%s", "%s"] }
                            """.trimIndent(),
                            inputRequest.did!!,
                            Base64.getEncoder().encodeToString(givenHash1),
                            Base64.getEncoder().encodeToString(givenHash2),
                            Base64.getEncoder().encodeToString(givenHash3)
                        )
                    )
                    val expectedData =
                        """
                            { "credentialSubject": $expectedSubject }
                        """
                    issuanceWireMock.verify(
                        WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                            .withRequestBody(
                                WireMock.matchingJsonPath(
                                    "$.credentialData",
                                    WireMock.equalToJson(expectedData)
                                )
                            )
                    )
                })
    }

    @Throws(IllegalArgumentException::class)
    private fun mergeJson(inputRequest: String, givenStaticTemplate: String): String {
        val expectedData = try {
            mapper.readerForUpdating(mapper.readTree(inputRequest))
                .readTree(givenStaticTemplate)
                .toPrettyString()
        } catch (ex: JsonProcessingException) {
            throw IllegalArgumentException(ex)
        }
        return expectedData
    }

    fun someProfileWithDocumentTemplate(documentTemplate: String?): Profile {
        return someProfileWithDocumentTemplate(AipVersion.V1_0, documentTemplate)
    }

    fun someProfileWithDocumentTemplate(aip: AipVersion, documentTemplate: String?): Profile {
        return Profile(
            UUID.randomUUID().toString(),
            aip,
            aip.asCredentialKind(),
            "name+" + UUID.randomUUID(),
            "description" + UUID.randomUUID(),
            setOf(UUID.randomUUID().toString()),
            "SomeValue",
            listOf(),
            Period.ofWeeks(3),
            true,
            mapper.readTree("""
                {}
            """.trimIndent()),
            documentTemplate,
            listOf(),
            ProfileTaskTree(),
            ProfileTaskTree(),
            ProfileTaskTree(),
            listOf(),
            listOf()
        )
    }

    fun someDocument(hash: ByteArray?): Document {
        return someDocument(UUID.randomUUID(), hash)
    }

    fun someDocument(id: UUID?, hash: ByteArray?): Document {
        val result = Document()
        result.id = id
        result.content = DataGen.genBytes()
        result.hash = Hex.encodeHexString(hash)
        result.title = "Title:" + UUID.randomUUID()
        result.shortDescription = "ShortDesc:" + UUID.randomUUID()
        result.longDescription = "LongDesc:" + UUID.randomUUID()
        return result
    }

    fun someRequest(): NotarizationRequest {
        val resultSession = Session()
        resultSession.id = UUID.randomUUID().toString()
        resultSession.profileId = ProfileId("Some-Profile-Id")
        resultSession.accessToken = UUID.randomUUID().toString()
        resultSession.documents = Set.of()
        resultSession.createdAt = OffsetDateTime.now().minusHours(1)
        val resultRequest = NotarizationRequest()
        resultRequest.createdAt = resultSession.createdAt!!.plusMinutes(1)
        resultRequest.claimedBy = "Some Notary"
        resultRequest.data =
            """
                { "name": "${UUID.randomUUID()}" }
            """.trimIndent()
        resultRequest.did = "did:dad:" + UUID.randomUUID()
        resultRequest.id = UUID.randomUUID()
        resultRequest.lastModified = resultRequest.createdAt!!.plusMinutes(20)
        resultRequest.rejectComment = null
        resultRequest.requestorInvitationUrl = null
        resultRequest.session = resultSession
        resultSession.request = resultRequest
        return resultRequest
    }
}
