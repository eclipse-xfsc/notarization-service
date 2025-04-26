/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.gaiax.notarization.request_processing.management

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import eu.gaiax.notarization.*
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.BeginIssuanceResult
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.extensions.ExternalServicesWireMock
import eu.gaiax.notarization.request_processing.extensions.MockExternalServicesResource
import eu.gaiax.notarization.request_processing.infrastructure.messaging.FrontEndMessageService
import eu.gaiax.notarization.request_processing.infrastructure.messaging.MsgType
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.SsiIssuanceRestClient
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState.Notary
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogAccept
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogAssignCredentialOverride
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailForNotarizationRequestID
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.reactive.messaging.memory.InMemoryConnector
import io.smallrye.reactive.messaging.memory.InMemorySink
import io.vertx.core.json.JsonObject
import jakarta.enterprise.inject.Any
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import org.stringtemplate.v4.Interpreter
import java.net.URI
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@Tag("security")
@QuarkusTestResource(
    MockServicesLifecycleManager::class
)
@QuarkusTestResource(RabbitMqTestResourceLifecycleManager::class)
@QuarkusTestResource(
    MockSsiIssuanceLifecycleManager::class
)
@QuarkusTestResource(
    MockExternalServicesResource::class
)
class AcceptAvailableRequestsTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @SsiIssuanceWireMock
    lateinit var issuanceWireMock: WireMockServer

    @SsiIssuanceV2WireMock
    lateinit var issuanceV2WireMock: WireMockServer

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    @Any
    lateinit var connector: InMemoryConnector

    var outgoingRequestor: InMemorySink<JsonObject>? = null

    @ExternalServicesWireMock
    lateinit var externalServicesWireMock: WireMockServer

    class NotAndSessId(
        val notReqId: String,
        val sessId: String
    )

    private fun createRequestWithStateInDB(
        state: NotarizationRequestState,
        didSet: Boolean,
        profile: ProfileId
    ): NotAndSessId {
        val notReqId = UUID.randomUUID()
        val sessId = UUID.randomUUID()
        withTransactionAsync(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = sessId.toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            val nr = NotarizationRequest()
            nr.id = notReqId
            nr.session = sess
            if (didSet) {
                nr.did = "did.action:value"
                nr.requestorInvitationUrl = "some-invitation"
            }
            session.persist(sess).chain{ _ -> session.persist(nr) }
        }.await().indefinitely()
        return NotAndSessId(
            notReqId.toString(),
            sessId.toString()
        )
    }

    private fun assertStateInDB(id: String, state: NotarizationRequestState) {
        withTransactionAsync(
            sessionFactory
        ) { dbSession, tx ->
            dbSession.find(
                NotarizationRequest::class.java, UUID.fromString(id)
            ).invoke { notReq: NotarizationRequest ->
                MatcherAssert.assertThat(
                    notReq.session!!.state, Matchers.`is`(state)
                )
            }
        }.await().indefinitely()
    }

    @BeforeEach
    fun setup() {
        outgoingRequestor = connector!!.sink(FrontEndMessageService.outgoingRequestorRequestChanged)
        outgoingRequestor!!.clear()

        externalServicesWireMock.resetRequests()
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canAcceptNotarizationRequestSummaryById_NoDid(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(state, false, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        assertStateInDB(ids.notReqId, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept())
        )
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat(
            outgoingRequestor!!.received()[0].payload, Matchers.`is`<JsonObject>(
                JsonObject(
                    """
                        { "id": "${ids.sessId}", "msg": "${MsgType.REQUEST_ACCEPTED}", "payload": "" }
                    """.trimIndent()
                )
            )
        )
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenActionsCanAcceptNotarizationRequestSummaryById_didSet(state: NotarizationRequestState) {
        val profile = MockState.profileWithPreIssuanceActionsId
        val ids = createRequestWithStateInDB(state, true, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .contentType(ContentType.JSON)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)

        assertStateInDB(ids.notReqId, NotarizationRequestState.PRE_ACCEPTED)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept())
        )
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat<JsonObject>(
            outgoingRequestor!!.received()[0].payload, Matchers.`is`<JsonObject>(
                JsonObject(
                    """
                        { "id": "${ids.sessId}", "msg": "${MsgType.REQUEST_PRE_ACCEPTED}", "payload": null }
                    """.trimIndent()
                )
            )
        )
    }
    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canAcceptNotarizationRequestSummaryById_didSet(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(state, true, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .contentType(ContentType.JSON)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        issuanceWireMock!!.verify(
            WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "$.successURL",
                        WireMock.containing("finishNotarizationRequest")
                    )
                )
        )
        assertStateInDB(ids.notReqId, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept())
        )
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat<JsonObject>(
            outgoingRequestor!!.received()[0].payload, Matchers.`is`<JsonObject>(
                JsonObject(
                    """
                        { "id": "${ids.sessId}", "msg": "${MsgType.REQUEST_ACCEPTED}", "payload": "" }
                    """.trimIndent()
                )
            )
        )
    }

    private val resp: SsiIssuanceRestClient.IssuanceResponse
        private get() {
            val resp = SsiIssuanceRestClient.IssuanceResponse()
            resp.invitationURL = URI.create("http://a.url")
            return resp
        }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @Throws(
        JsonProcessingException::class
    )
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canAcceptNotarizationRequestSummaryById_didSet_withInviteFromSSI(state: NotarizationRequestState) {
        issuanceWireMock!!.stubFor(
            WireMock.post(WireMock.urlMatching("/credential/start-issuance/"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
                        .withBody(objectMapper!!.writeValueAsString(resp))
                )
        )
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(state, true, profile)
        withTransaction<Int>(sessionFactory) { sess, tx ->
            val q =
                sess.createMutationQuery("update NotarizationRequest nr set nr.requestorInvitationUrl = '' where nr.id = :id")
            q.setParameter("id", UUID.fromString(ids.notReqId))
            q.executeUpdate()
        }
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        issuanceWireMock!!.verify(
            WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "$.successURL",
                        WireMock.containing("finishNotarizationRequest")
                    )
                )
        )
        assertStateInDB(ids.notReqId, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept())
        )
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat(
            outgoingRequestor!!.received()[0].payload, Matchers.`is`<JsonObject>(
                JsonObject(
                    """
                        { "id": "${ids.sessId}", "msg": "${MsgType.REQUEST_ACCEPTED}", "payload": "http://a.url" }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    fun acceptWithManualIssuanceLeadsToPendingRequestorReleaseAndCanBeReleased() {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, true, profile)
        val token = UUID.randomUUID().toString()
        //simulate manual release set
        withTransaction<Int>(sessionFactory) { sess: Mutiny.Session, tx: Mutiny.Transaction? ->
            val q = sess.createMutationQuery("update Session s set s.manualRelease = :relFlag where s.id = :id")
            q.setParameter("relFlag", true)
            q.setParameter("id", ids.sessId)
            val q2 = sess.createMutationQuery("update Session s set s.manualReleaseToken = :token where s.id = :id")
            q2.setParameter("token", token)
            q2.setParameter("id", ids.sessId)
            q.executeUpdate()
                .chain<Int> { s: Int? -> q2.executeUpdate() }
        }
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        assertStateInDB(ids.notReqId, NotarizationRequestState.PENDING_RQUESTOR_RELEASE)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept())
        )

        //call manualRelease
        RestAssured.given()
            .`when`()
            .post("/api/v1/session/triggerIssuance/{token}", token)
            .then()
            .statusCode(204)
        assertStateInDB(ids.notReqId, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat<JsonObject>(
            outgoingRequestor!!.received()[0].payload, Matchers.`is`<JsonObject>(
                JsonObject(
                    String.format(
                        """
                            { "id": "${ids.sessId}", "msg": "${MsgType.REQUEST_ACCEPTED}", "payload": "" }
                        """.trimIndent()
                    )
                )
            )
        )
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    fun canNotAcceptNotarizationRequestSummaryById(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(state, true, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(400)
        assertStateInDB(ids.notReqId, state)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept().httpStatus(400))
        )
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.empty()))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotAcceptNotarizationRequestWithUnknownId() {
        val profile = MockState.someProfileId
        createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, true, profile)
        val unkownId = UUID.randomUUID().toString()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", unkownId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(404)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(unkownId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept().httpStatus(404))
        )
        MatcherAssert.assertThat(outgoingRequestor!!.received(), Matchers.`is`(Matchers.empty()))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00031")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun acceptWithNotaryPayloadHasPayload() {
        Interpreter.trace = true

        val profile = MockState.augmentingProfileId
        val ids = createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, true, profile)

        /*var inputJson = String.format(
                """
                { "notaryValues" : "{ \"uniqueTag\": \"%s\" }" }
                """,
                UUID.randomUUID()); */
        val inputUniqueTag = UUID.randomUUID()
        val inputJson =
            """
                { "notaryValues" : { "uniqueTag": "$inputUniqueTag" } }
            """.trimIndent()
        val notary = MockState.notary4
        RestAssured.given()
            .header("Authorization", notary.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .contentType(ContentType.JSON)
            .body(inputJson)
            .`when`()
            .put(CRED_AUGMENT_PATH)
            .then()
            .statusCode(204)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(
                ids.notReqId,
                NotarizationRequestAction.CREDENTIAL_AUGMENTATION_PUT,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogAssignCredentialOverride())
        )
        expectAccept(ids, profile, notary)
        assertStateInDB(ids.notReqId, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(ids.notReqId, NotarizationRequestAction.ACCEPT, 1, sessionFactory),
            hasAuditEntries(auditLogAccept())
        )
        issuanceWireMock.verify(
            WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "$.credentialData.givenData.notaryValues.uniqueTag",
                        WireMock.equalTo(inputUniqueTag.toString())
                    )
                )
        )
    }

    private fun expectAccept(ids: NotAndSessId, profile: ProfileId, notary: Notary) {
        RestAssured.given()
            .header("Authorization", notary.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/accept"
        const val CRED_AUGMENT_PATH =
            "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/credentialAugmentation"

        @JvmStatic
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.WORK_IN_PROGRESS
            )
        }

        @JvmStatic
        fun notAllowedStates(): Stream<NotarizationRequestState> {
            val updateable = allowedStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !updateable.contains(s) }
        }
    }
}
