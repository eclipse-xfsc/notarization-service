package eu.gaiax.notarization.request_processing.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.application.domain.SessionInfo
import eu.gaiax.notarization.request_processing.application.taskprocessing.WorkExecutionEngine
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskInstance
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import eu.gaiax.notarization.request_processing.domain.services.RequestNotificationService
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.RequestorAuditingFilter
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.LockModeType
import jakarta.ws.rs.core.UriInfo
import mu.KotlinLogging
import org.hibernate.reactive.mutiny.Mutiny
import org.jose4j.lang.JoseException
import java.net.URI
import java.util.*

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class WorkService(
    val registry: MeterRegistry,
    val objectMapper: ObjectMapper,
    val profileService: ProfileService,
    val notifications: RequestNotificationService,
    val request: HttpServerRequest,
    val workEngine: WorkExecutionEngine,
) {

    @WithTransaction
    fun startTask(sessionInfo: SessionInfo, taskId: TaskId, data: JsonNode?): Uni<TaskInstance> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith {  NotFoundException("Session") }
            .call { s -> Mutiny.fetch(s!!.tasks) }
            .chain { dbSession ->
                NotarizationRequestStore.checkAccessToken(dbSession!!, sessionInfo)
                if (!dbSession.state!!.isTaskProcessingAllowed) {
                    throw InvalidRequestStateException(
                        sessionInfo.id,
                        NotarizationRequestState.taskProcessingStates,
                        dbSession.state
                    )
                }
                val sessionTask = dbSession.tasks!!.firstOrNull { Objects.equals(it.taskId, taskId.id) && it.workType == WorkType.Task }
                    ?: throw NotFoundException("Task")

                RequestorAuditingFilter.storeTaskName(request, sessionTask.name)
                profileService.find(dbSession.profileId!!)
                    .onItem().ifNotNull()
                    .transformToUni { p ->

                        //precondition is not fulfilled and asked task is none of precondition -> leads to error
                        if (!p!!.preconditionTasks.treeFulfilledBySession(dbSession)
                            &&
                            !p.preconditionTasks.containsTaskByName(sessionTask.name!!)
                        ) {
                            throw InvalidTaskStateException("Task state not valid")
                        }
                        workEngine.startWork(taskId, p, data)
                            .invoke { task: TaskInstance ->
                                notifications.onExternalTask(
                                    sessionInfo.id,
                                    task.uri
                                )
                            }
                    }
            }
    }

    @WithTransaction
    fun startAction(profileId: ProfileId, requestId: NotarizationRequestId, taskId: TaskId, data: JsonNode?, uriInfo: UriInfo): Uni<TaskInstance> {
        return NotarizationRequest.findById(requestId)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req: NotarizationRequest? ->
                val dbSession = req!!.session!!
                if (dbSession.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }

                if (!dbSession.state!!.isActionProcessingAllowed) {
                    throw InvalidRequestStateException(
                        SessionId(dbSession.id!!),
                        NotarizationRequestState.taskProcessingStates,
                        dbSession.state
                    )
                }
                val sessionTask = dbSession.tasks!!.firstOrNull { Objects.equals(it.taskId, taskId.id) && it.workType == WorkType.Action }
                    ?: throw NotFoundException("Task")

                RequestorAuditingFilter.storeActionName(request, sessionTask.name)
                profileService.find(dbSession.profileId!!)
                    .onItem().ifNotNull()
                    .transformToUni { profile ->

                        //precondition is not fulfilled and asked task is none of precondition -> leads to error
                        if (!profile!!.preIssuanceActions.containsTaskByName(sessionTask.name!!)) {
                            throw InvalidTaskStateException("Task state not valid")
                        }
                        workEngine.startWork(taskId, profile, data)
                            .invoke { task: TaskInstance ->
                                notifications.onExternalTask(
                                    SessionId(dbSession.id!!),
                                    task.uri
                                )
                            }
                    }
            }
    }

    @WithTransaction
    fun cancelTask(sessionInfo: SessionInfo, taskId: TaskId): Uni<Void> {
        return Session.findById(sessionInfo.id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .call { s -> Mutiny.fetch(s!!.tasks) }
            .chain { dbSession ->
                NotarizationRequestStore.checkAccessToken(dbSession!!, sessionInfo)
                if (!dbSession.state!!.isTaskProcessingAllowed) {
                    throw InvalidRequestStateException(
                        sessionInfo.id,
                        NotarizationRequestState.taskProcessingStates,
                        dbSession.state
                    )
                }
                val sessionTask = dbSession.tasks?.firstOrNull { Objects.equals(it.taskId, taskId.id) && it.workType == WorkType.Task }
                    ?: throw NotFoundException("Task")

                RequestorAuditingFilter.storeTaskName(request, sessionTask.name)
                profileService.find(dbSession.profileId!!)
                    .onItem().ifNull().failWith{ UnconfiguredProfileException(dbSession.profileId!!, "Cannot cancel task with unknown profile") }
                    .onItem().ifNotNull()
                    .transformToUni { profile ->
                        workEngine.cancelWork(taskId, profile!!, sessionTask)
                    }
            }
    }

    @WithTransaction
    fun cancelAction(profileId: ProfileId, requestId: NotarizationRequestId, taskId: TaskId, data: JsonNode?, uriInfo: UriInfo): Uni<Void> {
        return NotarizationRequest.findById(requestId)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req ->
                val dbSession = req!!.session!!
                if (dbSession.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!dbSession.state!!.isActionProcessingAllowed) {
                    throw InvalidRequestStateException(
                        SessionId(dbSession.id!!),
                        NotarizationRequestState.taskProcessingStates,
                        dbSession.state
                    )
                }
                Mutiny.fetch(dbSession.tasks)
                    .chain { tasks ->
                        val sessionTask = tasks!!.firstOrNull { Objects.equals(it.taskId, taskId.id) && it.workType == WorkType.Action }
                            ?: throw NotFoundException("Task")

                        RequestorAuditingFilter.storeTaskName(request, sessionTask.name)
                        profileService.find(dbSession.profileId!!)
                            .onItem().ifNull().failWith{ UnconfiguredProfileException(dbSession.profileId!!, "Cannot cancel task with unknown profile") }
                            .onItem().ifNotNull()
                            .transformToUni { profile ->
                                workEngine.cancelWork(taskId, profile!!, sessionTask)
                            }
                    }

            }
    }

    @WithTransaction
    fun listActions(profileId: ProfileId, requestId: NotarizationRequestId, request: UriInfo): Uni<List<TaskInstance>> {
        return NotarizationRequest.findById(requestId)
            .onItem().ifNull().failWith { NotFoundException("NotarizationRequest") }
            .onItem().ifNotNull()
            .transformToUni { req ->
                val dbSession = req!!.session!!
                if (dbSession.profileId!!.id != profileId.id) {
                    throw NotFoundException("The given request could not be found.")
                }
                if (!dbSession.state!!.isActionProcessingAllowed) {
                    throw InvalidRequestStateException(
                        SessionId(dbSession.id!!),
                        NotarizationRequestState.taskProcessingStates,
                        dbSession.state
                    )
                }
                Mutiny.fetch(dbSession.tasks)
                    .chain { actions ->
                        val currentActions = actions!!.filter { it.workType === WorkType.Action && it.running && !it.fulfilled }
                            .associateBy { it.taskId!! }

                        OngoingTask.find("taskId in ?1", currentActions.keys)
                            .list()
                            .map { ongoingActions ->

                                ongoingActions.map { ongoingAction ->
                                    TaskInstance(TaskId(ongoingAction.taskId!!),
                                        ongoingAction.invitationUri?.let { URI.create(it) }, currentActions[ongoingAction.taskId!!]?.name)
                                }
                            }
                    }
            }
    }

    @WithTransaction
    fun assignIdentity(sessionId: SessionId, body: String?, task: SessionTask, profile: Profile, workType: WorkType, encryptContent: Boolean, successful: Boolean): Uni<Void> {
        if (body.isNullOrBlank()) {
            throw BadParameterException("Identity data was empty.")
        }
        return Session.findById(sessionId.id, LockModeType.NONE)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .onItem().ifNotNull().transformToUni { dbSession ->
                val profileId = dbSession.profileId!!
                Mutiny.fetch<Any>(dbSession.identities).invoke { _ ->
                    if (encryptContent) {
                        for (notary in profile.notaries) {
                            try {
                                val requestor = RequestorIdentity()
                                requestor.id = UUID.randomUUID()
                                requestor.taskId = TaskId(task.taskId!!)
                                requestor.taskName = task.name
                                requestor.workType = workType
                                requestor.data = notary.encrypt(profile.encryption, body)
                                requestor.session = dbSession
                                requestor.jwk = notary.key.toJson()
                                requestor.encryption = profile.encryption
                                requestor.algorithm = notary.algorithm
                                requestor.successful = successful
                                dbSession.identities!!.add(requestor)
                            } catch (ex: JoseException) {
                                logger.error("Could not encrypt the identity", ex)
                            }
                        }
                    }
                    else {
                        val requestor = RequestorIdentity()
                        requestor.id = UUID.randomUUID()
                        requestor.taskId = TaskId(task.taskId!!)
                        requestor.taskName = task.name
                        requestor.workType = workType
                        requestor.data = body
                        requestor.session = dbSession
                        requestor.successful = successful
                        dbSession.identities!!.add(requestor)
                    }
                }.eventually { dbSession.persist<Session>() }
                    .onItem().invoke { _ ->
                        registry.counter("request.identity.success").increment()
                    }.replaceWithVoid()

            }
    }
    fun onVcIssuance(session: Session): Uni<Void> {
        return profileService.find(session.profileId!!)
            .chain { foundProfile ->
                if (foundProfile == null) {
                    logger.error { "While handling issuance of session ${session.id}, the profile with id ${session.profileId} could not be found" }
                }
                val relevantActions = foundProfile?.postIssuanceActions
                if (relevantActions?.isEmpty() != false) {
                    session.cleanup(workEngine, notifications)
                }
                else {
                    Mutiny.fetch(session.tasks)
                        .chain { tasks ->
                            val currentActions = tasks!!.filter { it.workType === WorkType.Action }.associateBy { it.name }
                            val peristOperations = mutableListOf<Uni<SessionTask>>()

                            for (actionName in relevantActions) {
                                var currentAction: SessionTask? = currentActions[actionName]
                                if (currentAction?.running == true) {
                                    continue
                                }
                                val operation = if (currentAction == null) {
                                    currentAction = SessionTask()
                                    with(currentAction) {
                                        taskId = UUID.randomUUID()
                                        name = actionName
                                        workType = WorkType.Action
                                    }
                                    session.tasks!!.add(currentAction)
                                    currentAction.persist<SessionTask>()
                                }
                                else {
                                    Uni.createFrom().item(currentAction)
                                }
                                peristOperations.add(operation)
                            }
                            Multi.createFrom().iterable(peristOperations)
                                .onItem().transformToUniAndConcatenate { sessionActionOperation ->
                                    sessionActionOperation.chain { sessionAction ->
                                        workEngine.startWork(sessionTask = sessionAction, profile = foundProfile, null)
                                            .onFailure().invoke { t ->
                                                logger.error(t) { "Notarization session ${session.id} could not begin post-issuance action ${sessionAction.name} for profile ${foundProfile.id}" }
                                            }
                                    }
                                }.collect().asList().invoke { items -> logger.info { "Begun total of ${items.size} post-issuance actions" } }
                                .replaceWithVoid()
                        }
                }
            }
    }

}
