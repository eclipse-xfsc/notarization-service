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
package eu.gaiax.notarization.request_processing.application

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdError
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.application.domain.SessionInfo
import eu.gaiax.notarization.request_processing.application.taskprocessing.WorkExecutionEngine
import eu.gaiax.notarization.request_processing.domain.entity.IssuanceProcess
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.domain.services.Interop
import eu.gaiax.notarization.request_processing.domain.services.IssuanceService
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import eu.gaiax.notarization.request_processing.domain.services.RequestNotificationService
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.NotarizationRequestView
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SessionSummary
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SubmitNotarizationRequest
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.CallbackAuditingFilter
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import io.vertx.pgclient.PgException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.persistence.LockModeType
import jakarta.persistence.PersistenceException
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hibernate.HibernateException
import org.hibernate.reactive.mutiny.Mutiny
import org.jboss.logging.Logger
import java.io.StringReader
import java.net.URI
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.Period
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class NotarizationRequestStore(
    private val logger: Logger,
    val registry: MeterRegistry,
    val objectMapper: ObjectMapper,
    val profileService: ProfileService,
    val notifications: RequestNotificationService,
    val notarizationManagement: NotarizationManagementStore,
    val issuanceService: IssuanceService,
    val request: HttpServerRequest,
    val workEngine: WorkExecutionEngine,
) {

    private var options: JsonLdOptions? = null
    var secureRandom: SecureRandom? = null
    fun onStartup(@Observes ev: StartupEvent?) {
        /*
         * Native builds optimise the application by initializing all instances at build time.
         *
         * However, SecureRandom will only correctly work in containers if initalized at runtime, using the relevant resources of the host machine.
         */
        secureRandom = SecureRandom()

        /*
         * SchemeRouter.INSTANCE initializes both new Threads and an instance of SecureRandom.
         */
        options = JsonLdOptions()
        options!!.setExplicit(true)
        options!!.setOrdered(true)
    }

    @WithTransaction
    fun createNewSession(profileId: ProfileId): Uni<Session> {
        return profileService.find(profileId)
            .onItem().ifNull().failWith { InvalidProfileException(profileId) }
            .onItem().ifNotNull().transformToUni { profile: Profile? ->
                Session.createNew(
                    SessionId(Interop.urlSafeString(secureRandom!!, ByteArray(32))),
                    AccessToken(Interop.urlSafeString(secureRandom!!, ByteArray(48))),
                    profile!!
                )
            }
    }

    @WithTransaction
    fun fetchSession(sessionInfo: SessionInfo): Uni<SessionSummary> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .onItem().ifNotNull()
            .call { s -> Mutiny.fetch(s!!.tasks) }
            .onItem().transformToUni { dbSession ->
                checkAccessToken(dbSession!!, sessionInfo)
                profileService.find(dbSession.profileId!!)
                    .onItem().ifNotNull()
                    .transform { p ->
                        val summary: SessionSummary = SessionSummary.asSummary(dbSession, p!!)
                        summary
                    }
            }
    }

    @WithTransaction
    fun softDeleteSession(session: SessionInfo): Uni<Void> {
        return Session.findById(session.id.id, LockModeType.NONE)
        .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { sess ->
                checkAccessToken(sess, session)
                if (!sess.state!!.isRevokeable) {
                    throw InvalidRequestStateException(
                        session.id,
                        NotarizationRequestState.revokeableStates,
                        sess.state
                    )
                }
                sess.state = NotarizationRequestState.TERMINATED
                sess.cleanup(workEngine, notifications)
            }
    }

    @ConfigProperty(name = "notarization-processing.terminated.session.retention.period")
    var retentionPeriod: Period? = null
    @WithTransaction
    fun pruneTerminatedSessions(): Uni<Long> {
        return Session.delete(
            "state in ?1 and lastModified < ?2",
            setOf(
                NotarizationRequestState.TERMINATED,
                NotarizationRequestState.ISSUED
            ),
            OffsetDateTime.now().minus(retentionPeriod)
        )
    }

    @ConfigProperty(name = "notarization-processing.session.timeout.period")
    var timeoutPeriod: Period? = null
    @WithTransaction
    fun pruneTimeoutSessions(): Uni<Int> {
        return pruneSessions(NotarizationRequestState.statesAffectedBySessionTimeout, timeoutPeriod)
    }

    @ConfigProperty(name = "notarization-processing.session.submission.timeout.period")
    var submissionTimeoutPeriod: Period? = null
    @WithTransaction
    fun pruneSubmissionTimeoutSessions(): Uni<Int> {
        return pruneSessions(NotarizationRequestState.statesAffectedBySubmissionTimeout, submissionTimeoutPeriod)
    }

    private fun pruneSessions(
        state: Set<NotarizationRequestState>,
        timeout: Period?
    ): Uni<Int> {
        val sessionQuery = Session.find(
            "state in ?1 and lastModified < ?2",
            state,
            OffsetDateTime.now().minus(timeout)
        ).list()
        return sessionQuery.chain { sessions ->
            Multi.createFrom().iterable(sessions).onItem().transformToUniAndConcatenate { sess ->
                sess.state = NotarizationRequestState.TERMINATED
                sess.cleanup(workEngine, notifications)
            }.collect().asList().map { sessions.size }
        }
    }

    @WithTransaction
    fun updateRequest(sessionInfo: SessionInfo, data: JsonNode): Uni<Void> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkAccessToken(session!!, sessionInfo)
                if (!session.state!!.isUpdateable) {
                    throw InvalidRequestStateException(
                        sessionInfo.id,
                        NotarizationRequestState.updateableStates,
                        session.state
                    )
                }
                val rawData = data.toString()
                assertValid(rawData)
                val request = session.request!!
                request.data = rawData
                request.persistAndFlush<NotarizationRequest>().replaceWithVoid()
            }
    }

    @WithTransaction
    fun submitNewRequest(
        id: NotarizationRequestId,
        sessionInfo: SessionInfo,
        submissionRequest: SubmitNotarizationRequest
    ): Uni<NotarizationRequest> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkAccessToken(session!!, sessionInfo)
                if (session.state !== NotarizationRequestState.SUBMITTABLE) {
                    throw InvalidRequestStateException(
                        SessionId(session.id!!),
                        NotarizationRequestState.SUBMITTABLE,
                        session.state!!
                    )
                }
                val rawData = submissionRequest.data.toString()
                assertValid(rawData)
                val newRequest = NotarizationRequest()
                newRequest.id = id.id
                newRequest.did = DistributedIdentity.valueOf(submissionRequest.holder)
                newRequest.requestorInvitationUrl = submissionRequest.invitation
                newRequest.session = session
                newRequest.data = rawData
                session.state = NotarizationRequestState.EDITABLE
                session.persist<Session>()
                    .chain { _ -> newRequest.persistAndFlush<NotarizationRequest>() }
            }.onFailure(PersistenceException::class.java).transform { ex ->
                var cause = ex.cause
                if (cause is HibernateException) {
                    cause = cause.cause
                    if (cause is PgException) {
                        val code: String = cause.getSqlState()
                        if ("23505" == code) {
                            return@transform CannotReuseTokenException("Cannot reuse token")
                        }
                    }
                }
                logger.error("Did not handle PersistenceException", ex)
                ex
            }
    }

    fun assertValid(data: String) {
        val document = try {
            JsonDocument.of(StringReader(data))
        } catch (ex: JsonLdError) {
            throw InvalidJsonLdException("Provided data is invalid", ex)
        }
        try {
            JsonLd.flatten(document).options(options).get()
        } catch (ex: JsonLdError) {
            throw InvalidJsonLdException("Provided data is invalid", ex)
        }
    }

    @WithTransaction
    fun markReady(sessionInfo: SessionInfo, manualRelease: Boolean): Uni<MarkReadyResponse> {
        return Session.findWithDocuments(sessionInfo.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain<MarkReadyResponse> { request ->
                val session: Session = request.session!!
                checkAccessToken(session, sessionInfo)
                if (!session.state!!.isMarkReadyAllowed) {
                    throw InvalidRequestStateException(
                        sessionInfo.id,
                        NotarizationRequestState.markReadyStates,
                        session.state
                    )
                }
                val profileId = session.profileId!!
                profileService.find(profileId)
                    .call { _ -> Mutiny.fetch<Set<SessionTask>>(session.tasks) }
                    .onItem().ifNotNull()
                    .transformToUni<MarkReadyResponse> { p ->
                        if (p!!.preconditionTasks.treeFulfilledBySession(session)
                            &&
                            p.tasks.treeFulfilledBySession(session)
                        ) {
                            session.state = NotarizationRequestState.READY_FOR_REVIEW
                            session.manualRelease = false
                            val releaseUrl = if (manualRelease) {
                                session.manualRelease = true
                                val noncedUri = buildManualReleaseUrl()
                                session.manualReleaseToken = noncedUri.nonce
                                noncedUri.uri
                            } else {
                                null
                            }
                            return@transformToUni session.persistAndFlush<Session>()
                                .invoke { s ->
                                    notifications.onReadyForReview(
                                        NotarizationRequestId(
                                            request.id!!
                                        ),
                                        profileId
                                    )
                                }
                                .replaceWith<MarkReadyResponse>(MarkReadyResponse(releaseUrl))
                        } else {
                            throw InvalidTaskStateException("Tasks not fulfilled.")
                        }
                    }
            }
    }

    private fun createNonce(): String {
        return Interop.urlSafeString(secureRandom!!, ByteArray(64))
    }

    inner class NoncedUri(
        val uri: URI,
        val nonce: String
    )

    @ConfigProperty(name = "notarization-processing.internal-url")
    var internalUrl: URI? = null

    private fun buildManualReleaseUrl(): NoncedUri {
        val nonce = createNonce()
        return NoncedUri(
            UriBuilder.fromUri(internalUrl)
                .path(Api.Path.SESSION_RESOURCE)
                .path(Api.Path.ISSUE_MANUALY)
                .path(nonce)
                .build(),
            nonce
        )
    }

    @WithTransaction
    fun markUnready(sessionInfo: SessionInfo): Uni<Void> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkAccessToken(session!!, sessionInfo)
                if (!session.state!!.isMarkUnreadyAllowed) {
                    throw InvalidRequestStateException(
                        sessionInfo.id,
                        NotarizationRequestState.markUnreadyStates,
                        session.state
                    )
                }
                session.state = NotarizationRequestState.EDITABLE
                session.persistAndFlush<Session>()
            }
            .replaceWithVoid()
    }

    @WithTransaction
    fun fetchNotarizationRequest(sessionInfo: SessionInfo): Uni<NotarizationRequestView> {
        return Session.findWithDocuments(sessionInfo.id)
            .map { res ->
                checkAccessToken(res.session!!, sessionInfo)
                NotarizationRequestView.from(
                    res!!,
                    objectMapper
                )
            }
    }

    @WithTransaction
    fun assignDidHolder(sessionInfo: SessionInfo, didHolder: String?, invitation: String?): Uni<Void> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkAccessToken(session!!, sessionInfo)
                val profileId = session.profileId!!
                val currentState = session.state!!
                profileService.find(profileId)
                    .onItem().ifNull().failWith { InvalidProfileException(profileId) }
                    .onItem().ifNotNull().transformToUni { profile ->
                        Mutiny.fetch(session.tasks)
                            .chain { tasks ->
                                val notarizationRequest = session.request!!
                                if (!currentState.isAssignDidAllowed || (notarizationRequest.hasHolderAccess(profile!!) &&
                                        (currentState === NotarizationRequestState.ACCEPTED || currentState === NotarizationRequestState.PRE_ACCEPTED))) {
                                    throw InvalidRequestStateException(
                                        sessionInfo.id,
                                        NotarizationRequestState.assignDidStates,
                                        currentState
                                    )
                                }
                                if (didHolder != null) {
                                    notarizationRequest.did = didHolder
                                }
                                if (invitation != null) {
                                    notarizationRequest.requestorInvitationUrl = invitation
                                }
                                val tasksByName = tasks!!.associateBy { it.name!! }
                                session.state = notarizationManagement.determineAcceptState(notarizationRequest, profile, tasksByName)

                                notarizationRequest.persist<NotarizationRequest>()
                                    .chain { _ -> session.persistAndFlush<Session>() }
                                    .chain { _ -> notarizationManagement.handlePostAcceptance(notarizationRequest, profile, currentState) }.replaceWithVoid()
                            }
                    }
            }
    }

    @WithTransaction
    fun getSsiInvitationUrl(sessionInfo: SessionInfo): Uni<List<IssuanceProcess>> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .onItem().ifNotNull().transformToUni { session ->
                checkAccessToken(session!!, sessionInfo)
                val state = session.state!!
                if (state !== NotarizationRequestState.ACCEPTED) {
                    throw InvalidRequestStateException(
                        sessionInfo.id,
                        NotarizationRequestState.ACCEPTED,
                        state
                    )
                }
                Mutiny.fetch(session.issuanceProcesses!!)
            }
    }

    @WithTransaction
    fun updateContact(sessionInfo: SessionInfo, contact: String?): Uni<Void> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .invoke { session ->
                checkAccessToken(session!!, sessionInfo)
                notifications.onContactUpdate(SessionId(session.id!!), contact)
            }.replaceWithVoid()
    }

    @WithTransaction
    fun manualRelease(releaseToken: String): Uni<Void> {
        return Session.find("manualReleaseToken = ?1 and state = ?2",
            releaseToken,
            NotarizationRequestState.PENDING_RQUESTOR_RELEASE
        ).firstResult()
        .onItem().ifNull().failWith { NotFoundException("Session") }
            .onItem().ifNotNull()
            .transformToUni { sess ->
                CallbackAuditingFilter.storeSessionId(request, sess!!.id)
                val profileId = sess.profileId!!
                profileService.find(profileId)
                    .onItem().ifNull().failWith { InvalidProfileException(profileId) }
                    .onItem().ifNotNull().transformToUni { profile ->
                        Mutiny.fetch(sess.tasks)
                            .chain { tasks ->
                                val previousState = sess.state!!
                                val tasksByName = tasks!!.associateBy { it.name!! }
                                sess.manualRelease = false
                                sess.state =
                                    notarizationManagement.determineAcceptState(sess.request!!, profile!!, tasksByName)
                                sess.persistAndFlush<Session>()
                                    .call { ->
                                        notarizationManagement.handlePostAcceptance(
                                            sess.request!!,
                                            profile,
                                            previousState
                                        )
                                    }
                            }
                    }
            }.replaceWithVoid()
    }

    companion object {
        @Throws(AuthorizationException::class)
        fun checkAccessToken(dbSession: Session, sessionInfo: SessionInfo) {
            if (dbSession.accessToken != sessionInfo.token.token) {
                throw AuthorizationException("Not authorized", sessionInfo.id)
            }
        }
    }
}
