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
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.MockSsiIssuanceLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditAssignDid
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
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
@QuarkusTestResource(MockSsiIssuanceLifecycleManager::class)
class AssignDidHolderTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun stateChangesIfPendingDid() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        Helper.setSessionState(
            sessionWithNotarizationRequest.session,
            NotarizationRequestState.PENDING_DID,
            sessionFactory
        )
        val holderVal = "did:action:newVal"
        val invitationVal = "did:this-is-another-invitation"
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam(HOLDER_VARIABLE, holderVal)
            .queryParam(INVITATION_VARIABLE, invitationVal)
            .`when`().put(ASSIGN_DID_HOLDER_PATH)
            .then()
            .statusCode(204)
        Helper.assertStateInStoredSession(
            sessionWithNotarizationRequest.session.id,
            sessionFactory,
            NotarizationRequestState.ACCEPTED
        )
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @Throws(
        JsonProcessingException::class
    )
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canAssignDidHolderToNotRequest(state: NotarizationRequestState) {
        val notarizationRequest = Helper.createSubmitNotarizationRequest()
        notarizationRequest.holder = null
        notarizationRequest.invitation = null
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(
            notarizationRequest, state, sessionFactory, MockState.someProfileId
        )
        Helper.setSessionState(sessionWithNotarizationRequest.session, state, sessionFactory)
        val holderVal = "did:action:newVal"
        val invitationVal = "did:this-is-another-invitation"
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam(HOLDER_VARIABLE, holderVal)
            .queryParam(INVITATION_VARIABLE, invitationVal)
            .`when`().put(ASSIGN_DID_HOLDER_PATH)
            .then()
            .statusCode(204)
        RestAssured.given().accept(ContentType.JSON)
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .`when`()[Helper.SUBMISSION_PATH]
            .then()
            .statusCode(200)
            .body("holder", CoreMatchers.`is`<String>(holderVal))
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.ASSIGN_DID,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditAssignDid())
        )
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @Throws(
        JsonProcessingException::class
    )
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun cannotAssignDidHolderToNotRequest(state: NotarizationRequestState) {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        Helper.setSessionState(sessionWithNotarizationRequest.session, state, sessionFactory)
        val holderVal = "did:action:newVal"
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam(HOLDER_VARIABLE, holderVal)
            .`when`().put(ASSIGN_DID_HOLDER_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.ASSIGN_DID,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditAssignDid().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun invalidTokenDetected() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val holderVal = "did:action:newVal"
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken + "INVALID")
            .queryParam(HOLDER_VARIABLE, holderVal)
            .`when`().put(ASSIGN_DID_HOLDER_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.ASSIGN_DID,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditAssignDid().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun noTokenDetected() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val holderVal = "did:action:newVal"
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .queryParam(HOLDER_VARIABLE, holderVal)
            .`when`().put(ASSIGN_DID_HOLDER_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.ASSIGN_DID,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditAssignDid().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun emptyTokenDetected() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val holderVal = "did:action:newVal"
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", "")
            .queryParam(HOLDER_VARIABLE, holderVal)
            .`when`().put(ASSIGN_DID_HOLDER_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.ASSIGN_DID,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditAssignDid().httpStatus(401))
        )
    }

    companion object {
        const val ASSIGN_DID_HOLDER_PATH = "/api/v1/session/{sessionId}/submission/did-holder"
        const val HOLDER_VARIABLE = "didHolder"
        const val INVITATION_VARIABLE = "invitation"

        @JvmStatic
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.EDITABLE,
                NotarizationRequestState.READY_FOR_REVIEW,
                NotarizationRequestState.WORK_IN_PROGRESS,
                NotarizationRequestState.PRE_ACCEPTED,
                NotarizationRequestState.ACCEPTED,
                NotarizationRequestState.PENDING_DID
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
