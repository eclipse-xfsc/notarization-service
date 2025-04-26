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
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSession
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSessionWithState
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogRevoke
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
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
    RabbitMqTestResourceLifecycleManager::class
)
class RevokeSubmissionTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @ParameterizedTest
    @MethodSource("revokeableStates")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    fun canRevokeRequestState(state: NotarizationRequestState?) {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val session = sessionWithNotarizationRequest.session
        val notarizationRequestDatabaseId = getNotaryRequestIdFromDb(sessionWithNotarizationRequest.session.id)
        val identityId = addIdentities(session.id)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`().delete(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(204)
        assertDeletionInDB(session.id, notarizationRequestDatabaseId, identityId)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.REVOKE, 1, sessionFactory),
            hasAuditEntries(auditLogRevoke())
        )
    }

    @ParameterizedTest
    @MethodSource("nonRevokeableStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun cannotRevokeRequestState(state: NotarizationRequestState) {
        val session = prepareSessionWithState(state, sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`().delete(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
            .body("actual", CoreMatchers.equalTo(state.toString()))
        assertExistanceInDB(session.id)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.REVOKE, 1, sessionFactory),
            hasAuditEntries(auditLogRevoke().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun noTokenIsDetected() {
        val session = prepareSession()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .`when`().delete(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.REVOKE, 1, sessionFactory),
            hasAuditEntries(auditLogRevoke().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun invalidTokenIsDetected() {
        val session = prepareSession()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", "INVALID_TOKEN")
            .`when`().delete(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.REVOKE, 1, sessionFactory),
            hasAuditEntries(auditLogRevoke().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun emptyTokenIsDetected() {
        val session = prepareSession()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", "")
            .`when`().delete(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.REVOKE, 1, sessionFactory),
            hasAuditEntries(auditLogRevoke().httpStatus(401))
        )
    }

    private fun getNotaryRequestIdFromDb(sessionId: String): UUID? {
        val res = withTransaction(
            sessionFactory
        ) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            session.find(
                Session::class.java, sessionId
            )
        }
        return res.request!!.id
    }

    private fun addIdentities(sessionId: String): UUID {
        val identiyId = UUID.randomUUID()
        withTransaction<Session>(
            sessionFactory
        ) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            val q = session.createSelectionQuery(
                "from Session s left join fetch s.identities where s.id = :id",
                Session::class.java
            )
            q.setParameter("id", sessionId)
            q.singleResult
                .call { s: Session ->
                    val identiy = RequestorIdentity()
                    identiy.id = identiyId
                    identiy.session = s
                    s.identities!!.add(identiy)
                    session.persist(s)
                }
        }
        return identiyId
    }

    private fun assertDeletionInDB(id: String, notaryReqId: UUID?, identityId: UUID) {
        withTransaction(
            sessionFactory
        ) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            val q = session.createSelectionQuery(
                "from Session s left join fetch s.identities where s.id = :id",
                Session::class.java
            )
            q.setParameter("id", id)
            q.singleResult
                .invoke { s: Session ->
                    MatcherAssert.assertThat(s.state, CoreMatchers.`is`(NotarizationRequestState.TERMINATED))
                    MatcherAssert.assertThat<Set<RequestorIdentity>>(s.identities, CoreMatchers.`is`(Matchers.empty()))
                }
        }
        val identity = withTransaction(
            sessionFactory
        ) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            session.find(
                RequestorIdentity::class.java, identityId
            )
        }
        MatcherAssert.assertThat(identity, CoreMatchers.nullValue())
        val notaryRequest = withTransaction(
            sessionFactory
        ) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            session.find(
                NotarizationRequest::class.java, notaryReqId
            )
        }
        MatcherAssert.assertThat(notaryRequest, CoreMatchers.nullValue())
    }

    private fun assertExistanceInDB(id: String) {
        val res = withTransaction(
            sessionFactory
        ) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            session.find(
                Session::class.java, id
            )
        }
        MatcherAssert.assertThat(res.id, CoreMatchers.`is`(id))
    }

    companion object {
        /*
    Sub set of states allowing revocation
     */
        @JvmStatic
        fun revokeableStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.READY_FOR_REVIEW,
                NotarizationRequestState.EDITABLE,
                NotarizationRequestState.CREATED
            )
        }

        /*
    all others
     */
        @JvmStatic
        fun nonRevokeableStates(): Stream<NotarizationRequestState> {
            val revokeable = revokeableStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !revokeable.contains(s) }
        }
    }
}
