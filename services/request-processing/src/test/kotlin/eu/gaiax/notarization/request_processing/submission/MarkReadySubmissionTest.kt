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
package eu.gaiax.notarization.request_processing.submission

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import eu.gaiax.notarization.*
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.assertStateInStoredSession
import eu.gaiax.notarization.request_processing.Helper.Companion.setSessionState
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.infrastructure.messaging.FrontEndMessageService
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.SsiIssuanceRestClient
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogManualRel
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogMarkReady
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.reactive.messaging.memory.InMemoryConnector
import jakarta.enterprise.inject.Any
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.net.MalformedURLException
import java.net.URI
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    MockSsiIssuanceLifecycleManager::class
)
@QuarkusTestResource(RabbitMqTestResourceLifecycleManager::class)
class MarkReadySubmissionTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @Inject
    @Any
    var connector: InMemoryConnector? = null

    @SsiIssuanceWireMock
    var issuanceWireMock: WireMockServer? = null

    @Inject
    lateinit var objectMapper: ObjectMapper

    @ConfigProperty(name = "quarkus.http.port")
    var assignedPort: Int = 0
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun canMarkReadyOnlyWithFulfilledTasks() {
        val results = connector!!.sink<String>(FrontEndMessageService.outgoingOperatorRequestChanged)
        results.clear()
        val session = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory).session
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(400)

        //simulate ready tasks
        withTransaction(sessionFactory!!) { sess, tx ->
            sess.createMutationQuery("update SessionTask st set st.fulfilled = true").executeUpdate()
        }
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(200)
        assertStateInStoredSession(session.id, sessionFactory!!, NotarizationRequestState.READY_FOR_REVIEW)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_READY, 2, sessionFactory),
            hasAuditEntries(auditLogMarkReady())
        )
        MatcherAssert.assertThat(results.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun cannotMarkReady(state: NotarizationRequestState) {
        val session = Helper.prepareSessionWithSubmittedNotarizationRequest(state, sessionFactory)
            .session
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(400)
        assertStateInStoredSession(session.id, sessionFactory!!, state)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_READY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkReady().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun invalidTokenDetectedInMarkReadyRequest() {
        val session =
            Helper.prepareSessionWithSubmittedNotarizationRequest(NotarizationRequestState.EDITABLE, sessionFactory)
                .session
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken + "INVALID")
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_READY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkReady().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun noTokenDetectedInMarkReadyRequest() {
        val session =
            Helper.prepareSessionWithSubmittedNotarizationRequest(NotarizationRequestState.EDITABLE, sessionFactory)
                .session
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_READY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkReady().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun emptyTokenDetectedInMarkReadyRequest() {
        val session =
            Helper.prepareSessionWithSubmittedNotarizationRequest(NotarizationRequestState.EDITABLE, sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.session.id)
            .header("token", "")
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(session.session.id, NotarizationRequestAction.MARK_READY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkReady().httpStatus(401))
        )
    }

    @Test
    @Throws(MalformedURLException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun canReleaseAcceptedRequest() {
        val results = connector!!.sink<String>(FrontEndMessageService.outgoingOperatorRequestChanged)
        results.clear()
        val pId = ProfileId(MockState.profile1.id)
        val session = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, pId)

        //simulate ready tasks
        withTransaction(sessionFactory!!) { sess, tx ->
            sess.createMutationQuery("update SessionTask st set st.fulfilled = true").executeUpdate()
        }
        val resp = RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.session.id)
            .header("token", session.session.accessToken)
            .queryParam("manualRelease", true)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(200)
            .extract().body().`as`<MarkReadyResponse>(MarkReadyResponse::class.java)
        val url = fixUrl(resp.releaseUrl!!)

        //url returns not found until notary accepts
        RestAssured.given()
            .`when`()
            .post(url)
            .then()
            .statusCode(404)

        //simulate claiming and accepting
        val reqId = withTransaction<UUID>(
            sessionFactory!!
        ) { sess, tx ->
            val q = sess.createSelectionQuery(
                "select id from NotarizationRequest where session_id = :sid",
                UUID::class.java
            )
            q.setParameter("sid", session.session.id)
            q.singleResult
        }
        setSessionState(session.session, NotarizationRequestState.WORK_IN_PROGRESS, sessionFactory)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", reqId)
            .pathParam("profileId", pId.id)
            .`when`()
            .post(ACCEPT_PATH)
            .then()
            .statusCode(204)
        RestAssured.given()
            .`when`()
            .post(url)
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
        assertStateInStoredSession(session.session.id, sessionFactory, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(results.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(session.session.id, NotarizationRequestAction.MANUAL_RELEASE, sessionFactory),
            hasAuditEntries(auditLogManualRel())
        )
    }

    private val resp: SsiIssuanceRestClient.IssuanceResponse
        private get() {
            val resp = SsiIssuanceRestClient.IssuanceResponse()
            resp.invitationURL = URI.create("http://a.url")
            return resp
        }

    @Test
    @Throws(MalformedURLException::class, JsonProcessingException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun canReleaseAcceptedRequestGettingInviteFromSsiIssuanceService() {
        val results = connector!!.sink<String>(FrontEndMessageService.outgoingOperatorRequestChanged)
        results.clear()
        issuanceWireMock!!.stubFor(
            WireMock.post(WireMock.urlMatching("/credential/start-issuance/"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString(resp))
                )
        )
        val pId = ProfileId(MockState.profile1.id)
        val session = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, pId)

        //simulate ready tasks
        withTransaction(sessionFactory) { sess, tx ->
            sess.createMutationQuery("update SessionTask st set st.fulfilled = true").executeUpdate()
        }
        val resp = RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.session.id)
            .header("token", session.session.accessToken)
            .queryParam("manualRelease", true)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(200)
            .extract().body().`as`(MarkReadyResponse::class.java)
        val url = fixUrl(resp.releaseUrl!!)

        //url returns not found until notary accepts
        RestAssured.given()
            .`when`()
            .post(url)
            .then()
            .statusCode(404)

        //simulate claiming and accepting
        val reqId = withTransaction<UUID>(
            sessionFactory
        ) { sess, tx ->
            val q = sess.createSelectionQuery(
                "select id from NotarizationRequest where session_id = :sid",
                UUID::class.java
            )
            q.setParameter("sid", session.session.id)
            q.singleResult
        }
        setSessionState(session.session, NotarizationRequestState.WORK_IN_PROGRESS, sessionFactory)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", reqId)
            .pathParam("profileId", pId.id)
            .`when`()
            .post(ACCEPT_PATH)
            .then()
            .statusCode(204)
        RestAssured.given()
            .`when`()
            .post(url)
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
        assertStateInStoredSession(session.session.id, sessionFactory, NotarizationRequestState.ACCEPTED)
        MatcherAssert.assertThat(results.received(), Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(session.session.id, NotarizationRequestAction.MANUAL_RELEASE, sessionFactory),
            hasAuditEntries(auditLogManualRel())
        )
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.session.id)
            .header("token", session.session.accessToken)
            .`when`()[Helper.SUBMISSION_PATH + "/ssiInviteUrl"]
            .then()
            .statusCode(200)
            .body("inviteUrl", hasItem("http://a.url"))
    }

    @Throws(MalformedURLException::class)
    private fun fixUrl(url: URI): String {
        return UriBuilder.fromUri(url).port(assignedPort).build().toURL().toString()
    }

    companion object {
        private const val MARK_READY_PATH = Helper.SUBMISSION_PATH + "/ready"

        @JvmStatic
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.EDITABLE
            )
        }

        @JvmStatic
        fun notAllowedStates(): Stream<NotarizationRequestState> {
            val updateable = allowedStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !updateable.contains(s) }
        }

        const val ACCEPT_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/accept"
    }
}
