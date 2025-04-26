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
package eu.gaiax.notarization

import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.domain.entity.Document
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.RequestorIdentity
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.jboss.logging.Logger
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.time.OffsetDateTime
import java.time.Period
import java.util.*
import java.util.List
import java.util.Set
import java.util.function.Supplier

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
class CleanupRoutinesTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @ConfigProperty(name = "notarization-processing.terminated.session.retention.period")
    lateinit var retentionPeriod: Period

    @ConfigProperty(name = "notarization-processing.session.timeout.period")
    lateinit var timeoutPeriod: Period

    @ConfigProperty(name = "notarization-processing.session.submission.timeout.period")
    lateinit var submissionTimeoutPeriod: Period

    private fun addSession(state: NotarizationRequestState, modDate: OffsetDateTime): UUID {
        val id = UUID.randomUUID()
        Helper.withTransaction(sessionFactory) { dbSess, ty ->
            val req = NotarizationRequest()
            req.id = UUID.randomUUID()
            val d = Document()
            d.id = UUID.randomUUID()
            val s = Session()
            s.request = req
            req.session = s
            d.session = s
            s.documents = mutableSetOf(d)
            s.id = id.toString()
            s.state = state
            dbSess
                .persist(s)
                .chain { _ -> dbSess.persist(req) }
                .chain { _ -> dbSess.persist(d) }
        }
        addIdentities(id.toString())
        Helper.withTransaction(sessionFactory) { dbSess: Mutiny.Session, tx: Mutiny.Transaction ->
            dbSess.createMutationQuery("update Session s set s.lastModified = ?1 where s.id = ?2")
                .setParameter(1, modDate)
                .setParameter(2, id.toString())
                .executeUpdate()
        }
        return id
    }

    private fun addIssuedOldEnoughSession(): UUID {
        return addSession(
            NotarizationRequestState.ISSUED,
            OffsetDateTime.now().minus(retentionPeriod).minus(Period.ofDays(1))
        )
    }

    private fun addTerminatedOldEnoughSession(): UUID {
        return addSession(
            NotarizationRequestState.TERMINATED,
            OffsetDateTime.now().minus(retentionPeriod).minus(Period.ofDays(1))
        )
    }

    private fun addNotTerminatedOldEnoughSession(): UUID {
        return addSession(
            NotarizationRequestState.EDITABLE,
            OffsetDateTime.now().minus(retentionPeriod).minus(Period.ofDays(1))
        )
    }

    private fun addIssuedNotOldEnoughSession(): UUID {
        return addSession(
            NotarizationRequestState.ISSUED,
            OffsetDateTime.now().minus(retentionPeriod).plus(Period.ofDays(1))
        )
    }

    private fun addTerminatedNotOldEnoughSession(): UUID {
        return addSession(
            NotarizationRequestState.TERMINATED,
            OffsetDateTime.now().minus(retentionPeriod).plus(Period.ofDays(1))
        )
    }

    private fun addNotTerminatedNotOldEnoughSession(): UUID {
        return addSession(
            NotarizationRequestState.EDITABLE,
            OffsetDateTime.now().minus(retentionPeriod).plus(Period.ofDays(1))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00013")
    fun routineDeletesTerminatedOrIssuedSessionsOlderThanRetentionPeriod() {
        val toDeleteSessions = listOf(
            addTerminatedOldEnoughSession(),
            addTerminatedOldEnoughSession(),
            addIssuedOldEnoughSession()
        )
        val notToDeleteSessions = listOf(
            addTerminatedNotOldEnoughSession(),
            addTerminatedNotOldEnoughSession(),
            addIssuedNotOldEnoughSession(),
            addNotTerminatedOldEnoughSession(),
            addNotTerminatedNotOldEnoughSession()
        )
        RestAssured.given()
            .`when`()
            .post(DEL_TERMINATED_PATH)
            .then()
            .statusCode(204)
        MatcherAssert.assertThat(toDeleteSessions, CoreMatchers.everyItem(notInDB))
        MatcherAssert.assertThat(notToDeleteSessions, CoreMatchers.everyItem(inDB))
    }

    private fun timedOutSession(state: NotarizationRequestState, p: Period?): UUID {
        return addSession(
            state,
            OffsetDateTime.now().minus(p).minus(Period.ofDays(1))
        )
    }

    private fun notTimedOutSession(state: NotarizationRequestState, p: Period?): UUID {
        return addSession(
            state,
            OffsetDateTime.now().minus(p).plus(Period.ofDays(1))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00013")
    fun routineDeletesTimeoutSessions() {
        val toDeleteSessions = List.of(
            timedOutSession(NotarizationRequestState.PENDING_DID, timeoutPeriod),
            timedOutSession(NotarizationRequestState.EDITABLE, timeoutPeriod)
        )
        val notToDeleteSessions = List.of(
            notTimedOutSession(NotarizationRequestState.PENDING_DID, timeoutPeriod),
            notTimedOutSession(NotarizationRequestState.EDITABLE, timeoutPeriod),
            timedOutSession(NotarizationRequestState.CREATED, timeoutPeriod),
            timedOutSession(NotarizationRequestState.SUBMITTABLE, timeoutPeriod),
            timedOutSession(NotarizationRequestState.ISSUED, timeoutPeriod)
        )
        RestAssured.given()
            .`when`()
            .post(DEL_TIMEOUT_PATH)
            .then()
            .statusCode(204)
        MatcherAssert.assertThat(toDeleteSessions, CoreMatchers.everyItem(sessionTerminated))
        MatcherAssert.assertThat(notToDeleteSessions, CoreMatchers.everyItem(sessionNotTerminated))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00013")
    fun routineDeletesSubmissionTimeoutSessions() {
        val toDeleteSessions = List.of(
            timedOutSession(NotarizationRequestState.CREATED, submissionTimeoutPeriod),
            timedOutSession(NotarizationRequestState.SUBMITTABLE, submissionTimeoutPeriod)
        )
        val notToDeleteSessions = List.of(
            notTimedOutSession(NotarizationRequestState.CREATED, submissionTimeoutPeriod),
            notTimedOutSession(NotarizationRequestState.SUBMITTABLE, submissionTimeoutPeriod),
            timedOutSession(NotarizationRequestState.EDITABLE, submissionTimeoutPeriod),
            timedOutSession(NotarizationRequestState.WORK_IN_PROGRESS, submissionTimeoutPeriod),
            timedOutSession(NotarizationRequestState.ISSUED, submissionTimeoutPeriod)
        )
        RestAssured.given()
            .`when`()
            .post(DEL_SUBMISSION_TIMEOUT_PATH)
            .then()
            .statusCode(204)
        MatcherAssert.assertThat(toDeleteSessions, CoreMatchers.everyItem(sessionTerminated))
        MatcherAssert.assertThat(notToDeleteSessions, CoreMatchers.everyItem(sessionNotTerminated))
    }

    private val sessionNotTerminated: BaseMatcher<UUID> = object : BaseMatcher<UUID>() {
        override fun matches(id: Any): Boolean {
            val s = Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                session.find(
                    Session::class.java, id.toString()
                )
            }
            return s != null && s.state !== NotarizationRequestState.TERMINATED
        }

        override fun describeTo(description: Description) {
            description.appendText("is a not terminated session in database")
        }

        override fun describeMismatch(item: Any, description: Description) {
            description.appendText("<$item> was not")
        }
    }
    private val inDB: BaseMatcher<UUID> = object : BaseMatcher<UUID>() {
        override fun matches(id: Any): Boolean {
            val s = Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                session.find(
                    Session::class.java, id.toString()
                )
            }
            return s != null
        }

        override fun describeTo(description: Description) {
            description.appendText("is an id found in database")
        }

        override fun describeMismatch(item: Any, description: Description) {
            description.appendText("<$item> was not")
        }
    }
    private val sessionTerminated: BaseMatcher<UUID> = object : BaseMatcher<UUID>() {
        private var reason = ""
        override fun matches(id: Any): Boolean {
            reason = ""
            val s = Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                session.find(
                    Session::class.java, id.toString()
                )
            }
            //we also check if identities are deleted just to be sure
            val identities_count =
                Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                    val q =
                        session.createNativeQuery<Long>("select count(id) from requestor_identity r where r.session_id = :sid")
                    q.setParameter("sid", id.toString())
                    q.singleResult
                }
            val documents_count =
                Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                    val q =
                        session.createNativeQuery<Long>(" select count(id) from document d where d.session_id = :sid")
                    q.setParameter("sid", id.toString())
                    q.singleResult
                }
            return if (s != null && s.state === NotarizationRequestState.TERMINATED) {
                if (identities_count.toInt() != 0) {
                    reason += "found identity, count was: $identities_count"
                    return false
                }
                if (documents_count.toInt() != 0) {
                    reason += "found documents with no association, count was: $documents_count"
                    return false
                }
                true
            } else {
                reason += if (s == null) {
                    "found no session"
                } else {
                    "found session with state: '" + s.state + "' instead of TERMINATED"
                }
                false
            }
        }

        override fun describeTo(description: Description) {
            description.appendText("a session with terminated state in database")
        }

        override fun describeMismatch(item: Any, description: Description) {
            description.appendText("<$item> was errornous, reason is: $reason")
        }
    }
    private val notInDB: BaseMatcher<UUID> = object : BaseMatcher<UUID>() {
        private var reason = ""
        override fun matches(id: Any): Boolean {
            reason = ""
            val s = Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                session.find(
                    Session::class.java, id.toString()
                )
            }
            //we also check if identities are deleted just to be sure
            val i = Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                val q =
                    session.createNativeQuery<Long>("select count(id) from requestor_identity r where r.session_id = :sid")
                q.setParameter("sid", id.toString())
                q.singleResult
            }
            return if (s == null) {
                if (i.toInt() == 0) {
                    true
                } else {
                    reason += "found identity, count was: $i"
                    false
                }
            } else {
                reason += "found session"
                false
            }
        }

        override fun describeTo(description: Description) {
            description.appendText("an id not found in database")
        }

        override fun describeMismatch(item: Any, description: Description) {
            description.appendText("<$item> was found reson is: $reason")
        }
    }

    private fun addIdentities(sessionId: String): UUID {
        val identiyId = UUID.randomUUID()
        Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
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

    companion object {
        private const val ROUTINES_PATH = "/api/v1/routines"
        private const val DEL_TERMINATED_PATH = ROUTINES_PATH + "/deleteTerminated"
        private const val DEL_TIMEOUT_PATH = ROUTINES_PATH + "/deleteTimeout"
        private const val DEL_SUBMISSION_TIMEOUT_PATH = ROUTINES_PATH + "/deleteSubmitTimeout"
    }
}
