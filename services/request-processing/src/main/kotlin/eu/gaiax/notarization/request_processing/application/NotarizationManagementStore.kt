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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.application.taskprocessing.WorkExecutionEngine
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.domain.services.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.IdentityView.Companion.from
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.CallbackAuditingFilter.Companion.storeSessionId
import io.quarkus.cache.CacheResult
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import mu.KotlinLogging
import org.hibernate.reactive.mutiny.Mutiny
import org.jboss.resteasy.reactive.ClientWebApplicationException
import java.net.URI
import java.net.URISyntaxException
import java.security.SecureRandom
import java.util.*

private val logger = KotlinLogging.logger { }

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class NotarizationManagementStore(
    private val objectMapper: ObjectMapper,
    private val profileService: ProfileService,
    private val notifications: RequestNotificationService,
    private val issuanceService: IssuanceService,
    private val revocationService: RevocationService,
    private val workEngine: WorkExecutionEngine,
    private val request: HttpServerRequest,
    private val workService: WorkService
) {
    private var secureRandom: SecureRandom? = null
    fun onStartup(@Observes ev: StartupEvent?) {
        /*
         * Native builds optimise the application by initializing all instances at build time.
         *
         * However, SecureRandom will only correctly work in containers if initalized at runtime, using the relevant resources of the host machine.
         */
        secureRandom = SecureRandom()
    }

    @WithTransaction
    @CacheResult(cacheName = "all-profile-roles")
    fun allRoles(): Uni<Map<String,Set<String>>> {
        return profileService.listProfileIdentifiers().flatMap { profileIdentiifers ->
            
            Multi.createFrom().iterable(profileIdentiifers)
                .onItem().transformToUniAndConcatenate { identifier ->

                     profileService.find(ProfileId(identifier))
                         .map {
                             identifier to it?.notaryRoles
                         }.invoke { roles ->
                             logger.debug { "Found roles for profile $identifier: $roles" }
                         }
                }.collect().asList().map { allRoles ->
                    val result = mutableMapOf<String, Set<String>>()
                    for (roles in allRoles) {
                        result[roles.first] = roles.second ?: setOf()
                    }
                    logger.debug { "All roles for profiles: $result" }
                    result
                }
        }
    }

    @WithTransaction
    fun fetchAvailableRequests(
        pageOffset: Int,
        limit: Int,
        filter: RequestFilter,
        securityContext: SecurityContext
    ): Uni<PagedNotarizationRequestSummary> {
        return allRoles().flatMap { profileIdentiifers ->
            logger.debug { "Checking available roles $profileIdentiifers"}
            val permittedProfiles: MutableSet<String> = HashSet(profileIdentiifers.size)
            for (profile in profileIdentiifers) {
                for (role in profile.value) {
                    if (securityContext.isUserInRole(role)) {
                        permittedProfiles.add(profile.key)
                        continue
                    }
                }
            }
            val availableRequests = fetchAvailNotarizationRequests(filter, permittedProfiles, securityContext.userPrincipal.name)
            val count = availableRequests.count()
            val page = availableRequests.page(pageOffset, limit)
            val pageEntries = page.list()
            val pageCount = page.pageCount()
            pageEntries.chain { pe: List<NotarizationRequestDbView> ->
                pageCount.chain { pc: Int ->
                    count.map { c: Long ->
                        val pagedNotarizationSummary = PagedNotarizationRequestSummary()
                        val results: MutableList<NotarizationRequestSummary> = ArrayList()
                        for (foundRequest in pe) {
                            results.add(
                                NotarizationRequestSummary.from(
                                    foundRequest, objectMapper
                                )
                            )
                        }
                        pagedNotarizationSummary.pageCount = pc
                        pagedNotarizationSummary.notarizationRequests = results
                        pagedNotarizationSummary.requestCount = c
                        pagedNotarizationSummary
                    }
                }
            }
        }
    }

    @WithTransaction
    fun fetchAvailableDocument(
        profileId: ProfileId,
        notReqId: NotarizationRequestId,
        docId: DocumentId
    ): Uni<DocumentFull> {
        return NotarizationRequest.findById(notReqId)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isFetchableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.fetchByNotaryStates, session.state!!
                    )
                }
                Document.findById(docId.id)
                    .map<DocumentFull> { obj: Document? -> DocumentFull.fromDocument(obj!!) }
            }
    }

    @WithTransaction
    fun fetchAvailableRequest(profileId: ProfileId, id: NotarizationRequestId): Uni<NotarizationRequestView> {
        return NotarizationRequest.findById(id.id)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isFetchableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.fetchByNotaryStates, session.state!!
                    )
                }
                req.loadDocuments().map { r: NotarizationRequest? ->
                    NotarizationRequestView.from(
                        req,
                        objectMapper
                    )
                }
            }
    }

    @WithTransaction
    fun claimAvailableRequest(
        profileId: ProfileId,
        id: NotarizationRequestId?,
        securityContext: SecurityContext
    ): Uni<Void> {
        return NotarizationRequest.findById(id!!)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isClaimableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.claimByNotaryStates, session.state!!
                    )
                }
                session.state = NotarizationRequestState.WORK_IN_PROGRESS
                req.claimedBy = securityContext.userPrincipal.name
                session.persist<Session>()
                    .chain { _ -> req.persist<NotarizationRequest>() }
            }.replaceWithVoid()
    }

    @WithTransaction
    fun rejectAvailableRequest(profileId: ProfileId, id: NotarizationRequestId?, rejectComment: String?): Uni<Void> {
        return NotarizationRequest.findById(id!!)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isRejectableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.rejectByNotaryStates, session.state!!
                    )
                }
                req.rejectComment = rejectComment
                session.state = NotarizationRequestState.EDITABLE
                session.persist<PanacheEntityBase>()
                    .chain { _ -> req.persist<NotarizationRequest>() }
                    .invoke { r: NotarizationRequest ->
                        notifications.onRejected(
                            SessionId(
                                r.session_id!!
                            )
                        )
                    }
            }.replaceWithVoid()
    }

    @WithTransaction
    fun deleteRequest(profileId: ProfileId, id: NotarizationRequestId): Uni<Void> {
        return NotarizationRequest.findById(id.id)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .chain<Void> { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isDeleteableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.deleteByNotaryStates, session.state!!
                    )
                }
                session.state = NotarizationRequestState.TERMINATED
                session.request = null
                Uni.createFrom().voidItem()
                    .chain { _ -> session.persistAndFlush<PanacheEntityBase>() }
                    .chain { _ -> RequestorIdentity.delete("session", session) }
                    .chain { _ -> req.delete() }
                    .chain { _ -> req.flush() }
                    .invoke { _ ->
                        notifications.onDeleted(
                            SessionId(
                                session.id!!
                            )
                        )
                    }
            }
    }

    @WithTransaction
    fun acceptAvailableRequest(profileId: ProfileId, id: NotarizationRequestId?): Uni<Void> {
        return NotarizationRequest.findById(id!!)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull().transformToUni { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isAcceptableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ),
                        NotarizationRequestState.acceptByNotaryStates, session.state!!
                    )
                }
                profileService.find(profileId)
                    .onItem().ifNull().failWith { InvalidProfileException(profileId) }
                    .onItem().ifNotNull()
                    .transformToUni { profile: Profile? ->
                        Mutiny.fetch(session.tasks)
                            .chain { tasks ->
                                val previousState = session.state!!
                                val tasksByName = tasks!!.associateBy { it.name!! }
                                session.state = determineAcceptState(req, profile!!, tasksByName)
                                val actionNames = profile.preIssuanceActions.allNames()

                                Multi.createFrom().iterable(actionNames)
                                    .onItem()
                                    .transformToUniAndConcatenate { actionName ->
                                        var sessionTask = tasksByName[actionName]
                                        if (sessionTask == null) {
                                            sessionTask = SessionTask()
                                            sessionTask.taskId = UUID.randomUUID()
                                            sessionTask.name = actionName
                                            sessionTask.workType = WorkType.Action
                                            sessionTask.fulfilled = false
                                            session.tasks!!.add(sessionTask)
                                        }
                                        workEngine.startWork(sessionTask, profile, null)
                                    }.collect().asList()
                                    .call { -> session.persist<Session>() }
                                    .call { -> req.persist<NotarizationRequest>() }
                                    .call { -> handlePostAcceptance(req, profile, previousState) }
                                }
                    }
            }.replaceWithVoid()
    }

    @WithTransaction
    fun getIdentity(profileId: ProfileId, id: NotarizationRequestId?): Uni<Set<IdentityView>> {
        return NotarizationRequest.findById(id!!)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { notReq: NotarizationRequest? ->
                val session = notReq!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isGetIdentityByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.fetchIdentityNotaryStates, session.state!!
                    )
                }
                session.loadIdentities()
                    .map { _ ->
                        session.identities!!.map { from(it) }.toSet()
                    }
            }
    }

    fun determineAcceptState(req: NotarizationRequest, profile: Profile, tasks: Map<String, SessionTask>): NotarizationRequestState {
        if (req.session!!.manualRelease == true) {
            return NotarizationRequestState.PENDING_RQUESTOR_RELEASE
        }
        val calcTree = TaskTreeForCalculating.buildTree(
            profile.preIssuanceActions,
            tasks
        )
        return if (calcTree.fulfilled()) {
            NotarizationRequestState.ACCEPTED
        } else {
            NotarizationRequestState.PRE_ACCEPTED
        }
    }

    fun handlePostAcceptance(
        request: NotarizationRequest,
        profile: Profile,
        previousState: NotarizationRequestState
    ): Uni<Void> {
        val session = request.session!!
        val sessionId = session.id!!
        val hasStateChanged = session.state != previousState

        return if (!hasStateChanged) {
            Uni.createFrom().voidItem()
        } else if (session.state === NotarizationRequestState.ACCEPTED) {
            issuanceService.issue(request, profile)
                .chain { results ->
                    Multi.createFrom().iterable(results)
                        .onItem().transformToUniAndConcatenate { response ->
                            val process = IssuanceProcess()
                            process.id = UUID.randomUUID().toString()
                            process.session = session
                            process.issuerVersion = response.apiVersion
                            process.ssiInvitationUrl = response.ssiInvitationUrl
                            process.failCBToken = response.failToken
                            process.successCBToken = response.successToken
                            process.successCBUri = response.successUri
                            process.failCBUri = response.failUri
                            process.persistAndFlush<IssuanceProcess>()
                        }.collect().asList().invoke { items ->
                            for (process in items) {
                                val invitation = process.ssiInvitationUrl
                                if (invitation != null) {
                                    notifications.onAccepted(
                                        SessionId(sessionId),
                                        invitation,
                                        process.issuerVersion!!.name,
                                    )
                                } else {
                                    notifications.onAccepted(SessionId(sessionId))
                                }
                            }
                            logger.debug { "Persisted results after beginning SSI issuance total: ${items.size}" }
                        }.replaceWithVoid()
                }
        } else if (session.state === NotarizationRequestState.PENDING_DID) {
            notifications.onPendingDid(SessionId(request.session_id!!))
            Uni.createFrom().voidItem()
        } else if (session.state === NotarizationRequestState.PRE_ACCEPTED) {
            notifications.onPreAccepted(SessionId(request.session_id!!))
            Uni.createFrom().voidItem()
        } else {
            Uni.createFrom().voidItem()
        }
    }

    @WithTransaction
    @Throws(URISyntaxException::class)
    fun revokeRequest(securityContext: SecurityContext, credential: JsonNode): Uni<Void> {
        val credentialStatus = credential.path("cred_value").path("credentialStatus")
        val listCredential = credentialStatus["statusListCredential"].textValue()
        val idx = java.lang.Long.valueOf(credentialStatus["statusListIndex"].textValue())
        return revocationService.getProfileName(getRevocationListName(listCredential))
            .onItem().ifNull().failWith { NotFoundException("Entry for list") }
            .onItem().ifNotNull().transformToUni { profileName: String? ->
                if (!securityContext.isUserInRole(profileName!!)) {
                    throw AuthorizationException("Revocation not allowed.")
                }
                revocationService.revoke(profileName, idx)
                    .onFailure(ClientWebApplicationException::class.java)
                    .transform { e: Throwable ->
                        val cause = e.cause
                        if (cause is WebApplicationException) {
                            if (cause.response.status == 404) {
                                return@transform NotFoundException("Index")
                            }
                        }
                        e
                    }
            }.replaceWithVoid()
    }

    @Throws(URISyntaxException::class)
    private fun getRevocationListName(listCredential: String): String {
        val path = URI(listCredential).path
        return path.substring(path.lastIndexOf("/") + 1, path.length)
    }

    @WithTransaction
    fun finishRequestSuccess(nonce: String): Uni<Void> {
        return IssuanceProcess.find(
            "successCBToken = ?1",
            nonce
        ).firstResult()
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .onItem().ifNotNull().transformToUni { process ->
                val s = process!!.session
                storeSessionId(request, s.id)
                if (s.state === NotarizationRequestState.ACCEPTED || s.state == NotarizationRequestState.TERMINATED) {
                    s.state = NotarizationRequestState.ISSUED
                }

                workService.onVcIssuance(s)
            }
    }

    @WithTransaction
    fun finishRequestFail(nonce: String): Uni<Void> {
        return IssuanceProcess.find(
            "failCBToken = ?1",
            nonce
        ).firstResult()
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .onItem().ifNotNull().transformToUni { process ->
                val s = process!!.session
                storeSessionId(request, s.id)
                if (s.state === NotarizationRequestState.ACCEPTED || s.state == NotarizationRequestState.TERMINATED) {
                    s.state = NotarizationRequestState.TERMINATED
                }

                workService.onVcIssuance(s)
            }
    }

    @WithTransaction
    fun assignCredentialAugmentation(
        profileId: ProfileId,
        id: NotarizationRequestId,
        credentialAugmentation: ObjectNode
    ): Uni<Void> {
        return NotarizationRequest.findById(id)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .chain { req: NotarizationRequest? ->
                val session = req!!.session!!
                if (session.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!session.state!!.isAcceptableByNotary) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.acceptByNotaryStates, session.state!!
                    )
                }
                req.credentialAugmentation = credentialAugmentation.toString()
                req.persist<NotarizationRequest>()
                    .replaceWithVoid()
            }
    }

    companion object {
        private fun fetchAvailNotarizationRequests(
            filter: RequestFilter,
            profileIds: Set<String>,
            notaryName: String
        ): PanacheQuery<NotarizationRequestDbView> {
            val states = statesByFilter(filter)
            return if (filter === RequestFilter.ownClaimed) {
                NotarizationRequestDbView.find(
                    "state in ?1 and profileId in ?2 and claimedBy = ?3",
                    states,
                    profileIds,
                    notaryName
                )
            } else {
                NotarizationRequestDbView.find(
                    "state in ?1 and profileId in ?2",
                    states,
                    profileIds
                )
            }
        }

        private fun statesByFilter(filter: RequestFilter): Set<NotarizationRequestState> {
            return when (filter) {
                RequestFilter.available -> setOf(NotarizationRequestState.READY_FOR_REVIEW)
                RequestFilter.allClaimed -> setOf(NotarizationRequestState.WORK_IN_PROGRESS)
                RequestFilter.ownClaimed -> setOf(NotarizationRequestState.WORK_IN_PROGRESS)
            }
        }
    }
}
