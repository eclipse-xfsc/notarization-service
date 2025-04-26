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
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogClaim
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailForNotarizationRequestID
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
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
class ClaimAvailableRequestsTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    private fun createRequestWithStateInDB(state: NotarizationRequestState, profile: ProfileId): String {
        val id = UUID.randomUUID()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = UUID.randomUUID().toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            val nr = NotarizationRequest()
            nr.id = id
            nr.session = sess
            session.persist(sess).chain{ _ -> session.persist(nr) }
        }.await().indefinitely()
        return id.toString()
    }

    private fun assertStateInDB(id: String, state: NotarizationRequestState) {
        withTransactionAsync(
            sessionFactory
        ) { dbSession, tx ->
            dbSession.find(
                NotarizationRequest::class.java, UUID.fromString(id)
            ).invoke { notReq: NotarizationRequest ->
                MatcherAssert.assertThat(
                    notReq.session!!.state, CoreMatchers.`is`(state)
                )
            }
        }.await().indefinitely()
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canClaimNotarizationRequestSummaryById(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(state, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", notRequestId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        assertStateInDB(notRequestId, NotarizationRequestState.WORK_IN_PROGRESS)
        withTransactionAsync<NotarizationRequest>(
            sessionFactory!!
        ) { dbSess, tx ->
            dbSess.find<NotarizationRequest>(
                NotarizationRequest::class.java, UUID.fromString(notRequestId)
            )
                .invoke { r -> assertThat(r.claimedBy, `is`(MockState.notary1.name)) }
        }.await().indefinitely()
        assertThat(
            auditTrailForNotarizationRequestID(notRequestId, NotarizationRequestAction.CLAIM, 1, sessionFactory!!),
            hasAuditEntries(auditLogClaim().httpStatus(204))
        )
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotClaimNotarizationRequestSummaryById(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(state, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", notRequestId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(400)
        assertStateInDB(notRequestId, state)
        assertThat(
            auditTrailForNotarizationRequestID(notRequestId, NotarizationRequestAction.CLAIM, 1, sessionFactory),
            hasAuditEntries(auditLogClaim().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotClaimNotarizationRequestSummaryByUnknownId() {
        val profile = MockState.someProfileId
        createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, profile)
        val unkownId = UUID.randomUUID().toString()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", unkownId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(404)
        assertThat(
            auditTrailForNotarizationRequestID(unkownId, NotarizationRequestAction.CLAIM, 1, sessionFactory!!),
            hasAuditEntries(auditLogClaim().httpStatus(404))
        )
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/claim"

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
