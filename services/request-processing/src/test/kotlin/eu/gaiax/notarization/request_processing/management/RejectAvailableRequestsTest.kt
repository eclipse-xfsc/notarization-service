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

import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogReject
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailForNotarizationRequestID
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.util.*
import java.util.function.Supplier
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
class RejectAvailableRequestsTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    private fun createRequestWithStateInDB(profile: ProfileId, state: NotarizationRequestState): String {
        val id = UUID.randomUUID()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = UUID.randomUUID().toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            val nr = NotarizationRequest()
            nr.id = id
            nr.session = sess
            session!!.persist(sess).chain { _ -> session.persist(nr) }
        }.await().indefinitely()
        return id.toString()
    }

    private fun assertStateAndCommentInDB(id: String, state: NotarizationRequestState, rejectComment: String?) {
        withTransactionAsync(
            sessionFactory
        ) { dbsession, tx ->
            dbsession.find(
                NotarizationRequest::class.java, UUID.fromString(id)
            ).invoke { notReq: NotarizationRequest ->
                MatcherAssert.assertThat(notReq.session!!.state, CoreMatchers.`is`(state))
                MatcherAssert.assertThat(notReq.rejectComment, CoreMatchers.`is`(rejectComment))
            }
        }.await().indefinitely()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00029")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canRejectNotarizationRequestWithoutComment() {
        val state = NotarizationRequestState.WORK_IN_PROGRESS
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(profile, state)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .contentType(ContentType.JSON)
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", notRequestId)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        assertStateAndCommentInDB(notRequestId, NotarizationRequestState.EDITABLE, null)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(notRequestId, NotarizationRequestAction.REJECT, 1, sessionFactory),
            hasAuditEntries(auditLogReject())
        )
    }

    @ParameterizedTest
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00029")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @MethodSource("allowedStates")
    fun canRejectNotarizationRequest(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(profile, state)
        val rejectComment = "rejectComment"
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .contentType(ContentType.JSON)
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", notRequestId)
            .body(prepareRejectComment(rejectComment))
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        assertStateAndCommentInDB(notRequestId, NotarizationRequestState.EDITABLE, rejectComment)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(notRequestId, NotarizationRequestAction.REJECT, 1, sessionFactory),
            hasAuditEntries(auditLogReject())
        )
    }

    @ParameterizedTest
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00029")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @MethodSource("notAllowedStates")
    fun canNotRejectNotarizationRequest(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(profile, state)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .contentType(ContentType.JSON)
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", notRequestId)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(400)
        assertStateAndCommentInDB(notRequestId, state, null)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(notRequestId, NotarizationRequestAction.REJECT, 1, sessionFactory),
            hasAuditEntries(auditLogReject().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00029")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotRejectNotarizationRequest() {
        val profile = MockState.someProfileId
        createRequestWithStateInDB(profile, NotarizationRequestState.WORK_IN_PROGRESS)
        val unkownId = UUID.randomUUID().toString()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .contentType(ContentType.JSON)
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", unkownId)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(404)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(unkownId, NotarizationRequestAction.REJECT, 1, sessionFactory),
            hasAuditEntries(auditLogReject().httpStatus(404))
        )
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/reject"

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

        private fun prepareRejectComment(comment: String): String {
            return """
                { "reason": "$comment" }
            """.trimIndent()
        }
    }
}
