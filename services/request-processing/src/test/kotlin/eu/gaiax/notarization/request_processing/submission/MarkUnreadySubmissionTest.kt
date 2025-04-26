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

import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.assertStateInStoredSession
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSessionWithState
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogMarkUnready
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
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
import java.util.function.BiFunction
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
class MarkUnreadySubmissionTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun canMarkunready(state: NotarizationRequestState?) {
        val session = prepareSessionWithState(state, sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()
            .post(MARK_UNREADY_PATH)
            .then()
            .statusCode(204)
        assertStateInStoredSession(session.id, sessionFactory, NotarizationRequestState.EDITABLE)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_UNREADY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkUnready())
        )
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun cannotMarkunready(state: NotarizationRequestState) {
        val session = prepareSessionWithState(state, sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()
            .post(MARK_UNREADY_PATH)
            .then()
            .statusCode(400)
        assertStateInStoredSession(session.id, sessionFactory, state)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_UNREADY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkUnready().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun invalidTokenDetectedInMarkunreadyRequest() {
        val session = prepareSessionWithState(NotarizationRequestState.EDITABLE, sessionFactory!!)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken + "INVALID")
            .`when`()
            .post(MARK_UNREADY_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_UNREADY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkUnready().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun noTokenDetectedInMarkunreadyRequest() {
        val session = prepareSessionWithState(NotarizationRequestState.EDITABLE, sessionFactory!!)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .`when`()
            .post(MARK_UNREADY_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_UNREADY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkUnready().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun emptyTokenDetectedInMarkunreadyRequest() {
        val session = prepareSessionWithState(NotarizationRequestState.EDITABLE, sessionFactory!!)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", "")
            .`when`()
            .post(MARK_UNREADY_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.MARK_UNREADY, 1, sessionFactory),
            hasAuditEntries(auditLogMarkUnready().httpStatus(401))
        )
    }

    fun assertSessionState(sess: Helper.SessionInfo, state: NotarizationRequestState) {
        val res = withTransaction(
            sessionFactory
        ) { session, tx ->
            session.find(
                Session::class.java, sess.id
            )
        }
        MatcherAssert.assertThat<NotarizationRequestState>(
            res.state,
            CoreMatchers.`is`<NotarizationRequestState>(state)
        )
    }

    companion object {
        private const val MARK_UNREADY_PATH = Helper.SUBMISSION_PATH + "/unready"

        @JvmStatic
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.READY_FOR_REVIEW
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
