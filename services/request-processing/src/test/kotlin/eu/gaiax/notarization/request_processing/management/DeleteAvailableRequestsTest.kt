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
import eu.gaiax.notarization.request_processing.DataGen.genString
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.RequestorIdentity
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogNotaryDelete
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailForNotarizationRequestID
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import eu.gaiax.notarization.request_processing.matcher.FieldMatcher
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Tag
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
@Tag("security")
@QuarkusTestResource(
    MockServicesLifecycleManager::class
)
@QuarkusTestResource(RabbitMqTestResourceLifecycleManager::class)
class DeleteAvailableRequestsTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    class Sess_Req_Identity_IDs(
        val sessId: UUID, val notarizationReqId: UUID, val identityId: UUID
    )

    private fun createRequestWithStateInDB(profile: ProfileId, state: NotarizationRequestState): Sess_Req_Identity_IDs {
        val notarizationReqId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val identityId = UUID.randomUUID()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = sessionId.toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            sess.identities = HashSet()
            val identity = RequestorIdentity()
            identity.id = identityId
            identity.data = genString()
            identity.session = sess
            sess.identities!!.add(identity)
            val nr = NotarizationRequest()
            nr.id = notarizationReqId
            nr.session = sess
            session.persist(sess).chain { _ -> session.persist(nr) }
        }.await().indefinitely()
        return Sess_Req_Identity_IDs(
            sessionId,
            notarizationReqId,
            identityId
        )
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = " CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canDeleteNotarizationRequest(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(profile, state)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", ids.notarizationReqId)
            .`when`()
            .delete(REQUEST_PATH)
            .then()
            .statusCode(204)
        assertSoftDeletionInDB(ids)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailForNotarizationRequestID(
                ids.notarizationReqId.toString(),
                NotarizationRequestAction.NOTARY_DELETE,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogNotaryDelete())
        )
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = " CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotRejectNotarizationRequest(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(profile, state)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", ids.notarizationReqId)
            .`when`()
            .delete(REQUEST_PATH)
            .then()
            .statusCode(400)
        assertExistInDb(ids)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailForNotarizationRequestID(
                ids.notarizationReqId.toString(),
                NotarizationRequestAction.NOTARY_DELETE,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogNotaryDelete().httpStatus(400))
        )
    }

    private fun assertExistInDb(ids: Sess_Req_Identity_IDs) {
        withTransaction<Session>(
            sessionFactory
        ) { session, tx ->
            val q = session.createSelectionQuery(
                "from Session s left join fetch s.identities where s.id = :id",
                Session::class.java
            )
            q.setParameter("id", ids.sessId.toString())
            q.singleResult
                .invoke { s: Session ->
                    MatcherAssert.assertThat<UUID?>(
                        s.request!!.id, CoreMatchers.`is`<UUID?>(ids.notarizationReqId)
                    )
                    MatcherAssert.assertThat<Set<RequestorIdentity>>(
                        s.identities,
                        Matchers.contains<RequestorIdentity>(
                            FieldMatcher.Companion.hasField<RequestorIdentity, UUID>(
                                "id",
                                CoreMatchers.`is`<UUID>(ids.identityId)
                            )
                        )
                    )
                }
        }
    }

    private fun assertSoftDeletionInDB(ids: Sess_Req_Identity_IDs) {
        withTransaction<Session>(
            sessionFactory
        ) { session, tx ->
            val q = session.createSelectionQuery(
                "from Session s left join fetch s.identities where s.id = :id",
                Session::class.java
            )
            q.setParameter("id", ids.sessId.toString())
            q.singleResult
                .invoke { s: Session ->
                    MatcherAssert.assertThat(s.state, CoreMatchers.`is`(NotarizationRequestState.TERMINATED))
                    MatcherAssert.assertThat<Set<RequestorIdentity>>(s.identities, CoreMatchers.`is`(Matchers.empty()))
                }
        }
        val identity = withTransaction(
            sessionFactory
        ) { session, tx ->
            session.find(
                RequestorIdentity::class.java, ids.identityId
            )
        }
        MatcherAssert.assertThat(identity, CoreMatchers.nullValue())
        val notaryRequest = withTransaction<NotarizationRequest?>(
            sessionFactory
        ) { session, tx ->
            session.find(
                NotarizationRequest::class.java, ids.notarizationReqId
            )
        }
        MatcherAssert.assertThat(notaryRequest, CoreMatchers.nullValue())
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}"

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
