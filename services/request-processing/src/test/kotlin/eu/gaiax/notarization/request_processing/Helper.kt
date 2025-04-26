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
package eu.gaiax.notarization.request_processing

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.DistributedIdentity
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SubmitNotarizationRequest
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import java.time.Duration
import java.util.function.BiFunction

/**
 *
 * @author Florian Otto
 */
class Helper {
    fun auditTrailFor(
        sessionId: String,
        action: NotarizationRequestAction,
        count: Int,
        sessionFactory: Mutiny.SessionFactory
    ): List<HttpNotarizationRequestAudit> {
        return AuditTrailMatcher.auditTrailFor(sessionId, action, count, sessionFactory)
    }

    class SessionInfo(
        val id: String,
        val location: String,
        val accessToken: String,
        val profileId: String
    )
    class SessionWithSubmittedNotarizationRequest(
        val session: SessionInfo,
        val notarizationRequest: SubmitNotarizationRequest
    )
    companion object {
        const val SESSION_VARIABLE = "sessionId"
        const val SUBMISSION_PATH = "/api/v1/session/{sessionId}/submission"
        const val SESSION_PATH = "/api/v1/session/{sessionId}"
        @JvmStatic
        fun prepareSession(profile: ProfileId = MockState.someProfileId): SessionInfo {
            val response = RestAssured.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(
                    java.lang.String.format(
                        """
                            {"profileId":"%s"}
                        """.trimIndent(),
                        profile.id
                    )
                )
                .`when`().post("/api/v1/session")
                .then()
                .extract()
            return SessionInfo(
                response.path<String>("sessionId"),
                response.header("Location"),
                response.path<String>("token"),
                profile.id
            )
        }

        fun prepareSessionWithSubmittedNotarizationRequest(
            factory: Mutiny.SessionFactory,
            profileId: ProfileId
        ): SessionWithSubmittedNotarizationRequest {
            val notarizationRequest = createSubmitNotarizationRequest()
            return prepareSessionWithSubmittedNotarizationRequest(notarizationRequest, factory, profileId)
        }

        fun prepareSessionWithSubmittedNotarizationRequest(factory: Mutiny.SessionFactory): SessionWithSubmittedNotarizationRequest {
            val notarizationRequest = createSubmitNotarizationRequest()
            return prepareSessionWithSubmittedNotarizationRequest(notarizationRequest, factory)
        }

        fun prepareSessionWithSubmittedNotarizationRequest(
            state: NotarizationRequestState, factory: Mutiny.SessionFactory
        ): SessionWithSubmittedNotarizationRequest {
            val notarizationRequest = createSubmitNotarizationRequest()
            return prepareSessionWithSubmittedNotarizationRequest(notarizationRequest, state, factory)
        }

        fun prepareSessionWithSubmittedNotarizationRequest(
            notarizationRequest: SubmitNotarizationRequest,
            factory: Mutiny.SessionFactory
        ): SessionWithSubmittedNotarizationRequest {
            return prepareSessionWithSubmittedNotarizationRequest(
                notarizationRequest,
                NotarizationRequestState.EDITABLE,
                factory
            )
        }

        fun prepareSessionWithSubmittedNotarizationRequest(
            notarizationRequest: SubmitNotarizationRequest,
            factory: Mutiny.SessionFactory,
            profileId: ProfileId
        ): SessionWithSubmittedNotarizationRequest {
            return prepareSessionWithSubmittedNotarizationRequest(
                notarizationRequest,
                NotarizationRequestState.EDITABLE,
                factory,
                profileId
            )
        }

        fun prepareSessionWithSubmittedNotarizationRequest(
            notarizationRequest: SubmitNotarizationRequest,
            state: NotarizationRequestState,
            factory: Mutiny.SessionFactory,
            profileId: ProfileId
        ): SessionWithSubmittedNotarizationRequest {
            val sessionInfo = prepareSessionWithState(NotarizationRequestState.SUBMITTABLE, factory, profileId)
            submitNotarizationRequest(notarizationRequest, sessionInfo)
            if (state !== NotarizationRequestState.EDITABLE) {
                setSessionState(sessionInfo, state, factory)
            }
            return SessionWithSubmittedNotarizationRequest(sessionInfo, notarizationRequest)
        }

        private fun prepareSessionWithSubmittedNotarizationRequest(
            notarizationRequest: SubmitNotarizationRequest,
            state: NotarizationRequestState,
            factory: Mutiny.SessionFactory
        ): SessionWithSubmittedNotarizationRequest {
            val sessionInfo = prepareSessionWithState(NotarizationRequestState.SUBMITTABLE, factory)
            submitNotarizationRequest(notarizationRequest, sessionInfo)
            if (state !== NotarizationRequestState.EDITABLE) {
                setSessionState(sessionInfo, state, factory)
            }
            return SessionWithSubmittedNotarizationRequest(sessionInfo, notarizationRequest)
        }

        fun prepareSessionWithState(
            state: NotarizationRequestState?,
            factory: Mutiny.SessionFactory,
            profileId: ProfileId
        ): SessionInfo {
            val sessionInfo = prepareSession(profileId)
            setSessionState(sessionInfo, state, factory)
            return sessionInfo
        }

        @JvmStatic
        fun prepareSessionWithState(state: NotarizationRequestState?, factory: Mutiny.SessionFactory): SessionInfo {
            val sessionInfo = prepareSession()
            setSessionState(sessionInfo, state, factory)
            return sessionInfo
        }

        @JvmStatic
        fun setSessionState(
            sessionInfo: SessionInfo,
            state: NotarizationRequestState?,
            factory: Mutiny.SessionFactory
        ) {
            withTransaction<Void>(factory) { dbSession: Mutiny.Session, transaction: Mutiny.Transaction? ->
                dbSession.find<Session>(
                    Session::class.java, sessionInfo.id
                )
                    .chain<Void> { foundSession: Session ->
                        foundSession.state = state
                        dbSession.persist(foundSession)
                    }
            }
        }

        @JvmStatic
        fun <T> withTransaction(
            factory: Mutiny.SessionFactory,
            work: BiFunction<Mutiny.Session, Mutiny.Transaction, Uni<T>>
        ): T {
            return factory.openSession()
                .chain { dbSession: Mutiny.Session ->
                    dbSession.withTransaction { transaction: Mutiny.Transaction -> work.apply(dbSession, transaction) }
                        .eventually<Void> { dbSession.close() }
                }
                .await().atMost(Duration.ofSeconds(10))
        }

        @JvmStatic
        fun <T> withTransactionAsync(
            factory: Mutiny.SessionFactory,
            work: BiFunction<Mutiny.Session, Mutiny.Transaction, Uni<T>>
        ): Uni<T> {
            return factory.openSession()
                .chain { dbSession: Mutiny.Session ->
                    dbSession.withTransaction { transaction: Mutiny.Transaction -> work.apply(dbSession, transaction) }
                        .eventually<Void> { dbSession.close() }
                }
        }

        @JvmStatic
        fun assertStateInStoredSession(
            sessId: String,
            factory: Mutiny.SessionFactory,
            expected: NotarizationRequestState
        ) {
            val dbSess = withTransaction(factory) { session, transaction ->
                session.find(
                    Session::class.java, sessId
                )
            }
            MatcherAssert.assertThat(dbSess.state, CoreMatchers.`is`(expected))
        }

        @JvmStatic
        @JvmOverloads
        fun createSubmitNotarizationRequest(data: JsonNode? = DataGen.createRandomJsonLdData()): SubmitNotarizationRequest {
            val submission = SubmitNotarizationRequest()
            submission.data = data
            submission.holder = DistributedIdentity("did:action:value")
            submission.invitation = "did:invitation:some-thing"
            return submission
        }

        private fun submitNotarizationRequest(inputRequest: SubmitNotarizationRequest, session: SessionInfo) {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .pathParam(SESSION_VARIABLE, session.id)
                .header("token", session.accessToken)
                .body(inputRequest)
                .`when`()
                .post(SUBMISSION_PATH)
                .then().statusCode(201)
        }
    }
}
