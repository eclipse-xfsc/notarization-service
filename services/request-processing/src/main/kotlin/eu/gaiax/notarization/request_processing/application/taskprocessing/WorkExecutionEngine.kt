package eu.gaiax.notarization.request_processing.application.taskprocessing

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.extensions.BeginTaskResponse
import eu.gaiax.notarization.api.extensions.ExtensionTaskServiceApi
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.application.WorkService
import eu.gaiax.notarization.request_processing.domain.entity.OngoingTask
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import eu.gaiax.notarization.request_processing.domain.model.TaskTreeForCalculating
import eu.gaiax.notarization.request_processing.domain.model.WorkType
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskInstance
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.CallbackAuditingFilter
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.RestClientBuilder
import org.hibernate.reactive.mutiny.Mutiny
import java.net.URI
import java.security.SecureRandom
import java.util.*

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class WorkExecutionEngine(
    private val externalServiceHandler: ExternalServiceHandlerProvider,
    private val internalServiceHandler: InternalServiceHandlerProvider,
    @param:ConfigProperty(name = "notarization-processing.internal-url")
    private val internalUrl: URI,
    private val request: HttpServerRequest,
    private val registry: MeterRegistry,
    private val workService: WorkService,
    private val profileService: ProfileService,
) {
    lateinit var secureRandom: SecureRandom

    fun onStartup(@Observes ev: StartupEvent) {
        secureRandom = SecureRandom()
    }

    @WithTransaction
    fun startWork(taskId: TaskId, profile: Profile, data: JsonNode?): Uni<TaskInstance> {
        return SessionTask.findById(taskId.id)
            .onItem().ifNull().failWith { NotFoundException("Task") }
            .chain { sessionTask ->
                startWork(sessionTask!!, profile, data)
            }
    }

    fun startWork(
        sessionTask: SessionTask,
        profile: Profile,
        data: JsonNode?
    ): Uni<TaskInstance> {
        val taskId = TaskId(sessionTask.taskId!!)
        sessionTask.running = true
        val (internalHandler, externalHandler) = findHandlers(profile, sessionTask)

        val nonce: String?

        val beginTaskOperation: Uni<BeginTaskResponse> = if (externalHandler != null) {
            nonce = createNonce()
            val beginExtensionTaskUri = externalHandler.createStartUri()

            val successCBUri = UriBuilder.fromUri(internalUrl)
                .path(Api.Path.FINISH_TASK_RESOURCE)
                .path(nonce)
                .path(Api.Path.SUCCESS)
                .build()
            val failCBUri = UriBuilder.fromUri(internalUrl)
                .path(Api.Path.FINISH_TASK_RESOURCE)
                .path(nonce)
                .path(Api.Path.FAIL)
                .build()

            val client = RestClientBuilder.newBuilder()
                .baseUri(beginExtensionTaskUri)
                .build(ExtensionTaskServiceApi::class.java)

            client.beginTask(successCBUri, failCBUri, profile.id, sessionTask.name!!, data)
        } else if (internalHandler != null) {
            nonce = null
            internalHandler.createStartUri(taskId).map { createdUri ->
                if (createdUri != null) {
                    val response = BeginTaskResponse(redirect = createdUri, cancel = null)
                    response
                } else {
                    null
                }
            }
        } else {
            throw UnconfiguredServiceException(profile.id, sessionTask.name)
        }
        return beginTaskOperation.chain { uris: BeginTaskResponse? ->
            OngoingTask.findById(taskId.id)
                .onItem().ifNotNull() //cancel ongoing if there is allready one
                .call { o -> cancelRemote(o?.cancelUri) }
                .onItem().ifNull() //create new one
                .continueWith {

                    val o = OngoingTask()
                    o.taskId = taskId.id
                    o
                }
                .chain { o ->
                    o!!.nonce = nonce
                    o.invitationUri = uris?.redirect?.toString()
                    o.cancelUri = uris?.cancel?.toString()
                    o.persistAndFlush<OngoingTask>()
                        .call { _ ->
                            sessionTask.running = true
                            sessionTask.persistAndFlush<SessionTask>()
                        }
                        .map { TaskInstance(TaskId(o.taskId!!), uris?.redirect) }
                }
        }
    }

    @WithTransaction
    fun cancelWork(taskId: TaskId, profile: Profile): Uni<Void> {
        return SessionTask.findById(taskId.id)
            .chain { sessionTask: SessionTask? ->
                if (sessionTask == null) {
                    throw NotFoundException("Task")
                }

                cancelWork(taskId, profile, sessionTask)
            }
    }

    @WithTransaction
    fun cancelWork(
        taskId: TaskId,
        profile: Profile,
        sessionTask: SessionTask
    ): Uni<Void> =
        OngoingTask.find("taskId", taskId.id)
            .firstResult()
            .onItem().ifNotNull()
            .transformToUni { o ->
                val cancelUri = o?.cancelUri
                if (cancelUri != null) {
                    cancelRemote(cancelUri)
                        .onFailure().invoke { t ->
                            logger.warn(t) { "Calling the extension service using the URI failed" }
                        }.map { o }
                } else {
                    Uni.createFrom().item(o)
                }
            }.chain { ongoingTask ->
                ongoingTask?.delete() ?: Uni.createFrom().nullItem()
            }.call { ->
                val (internalHandler, _) = findHandlers(profile, sessionTask)
                internalHandler?.cancelWork(taskId) ?: Uni.createFrom().voidItem()
            }.call { ->
                sessionTask.running = false
                sessionTask.fulfilled = false
                sessionTask.persistAndFlush<SessionTask>().replaceWithVoid()
            }

    @WithTransaction
    fun cancelWork(session: Session): Uni<Void> {
        return Uni.combine().all().unis(
            profileService.find(session.profileId!!),
            Mutiny.fetch(session.tasks)
        ).asTuple()
            .chain { tuple ->
                val profile = tuple.item1!!
                val sessionTasks = tuple.item2!!

                Multi.createFrom().iterable(sessionTasks)
                    .onItem()
                    .transformToUniAndConcatenate { sessTask ->
                        cancelWork(TaskId(sessTask.taskId!!), profile, sessTask)
                    }.collect().asList().invoke { item ->
                        logger.info { "Cancelld all tasks for session ${session.id}" }
                    }
        }.replaceWithVoid()
    }

    @WithTransaction
    fun finishWorkSuccess(taskId: TaskId, data: JsonNode?): Uni<Void?> {
        return SessionTask.findById(taskId.id)
            .onItem().ifNull().failWith { NotFoundException("TaskId") }
            .onItem().ifNotNull().invoke { sessTask: SessionTask? ->
                CallbackAuditingFilter.storeTaskName(request, sessTask!!.name)
                CallbackAuditingFilter.storeSessionId(request, sessTask.session!!.id)
            }
            .chain { sessTask: SessionTask? ->
                if (!sessTask!!.running) {
                    throw InvalidTaskStateException("Cannot finish not running task.")
                }
                sessTask.running = false
                val session = sessTask.session!!
                if (!session.state!!.isTaskProcessingAllowed) {
                    throw InvalidRequestStateException(
                        SessionId(
                            session.id!!
                        ), NotarizationRequestState.taskProcessingStates, session.state!!
                    )
                }
                profileService.find(session.profileId!!)
                    .chain { profile ->
                        val (internalHandler, externalHandler) = findHandlers(profile!!, sessTask)

                        val operation = if (externalHandler != null) {
                            workService.assignIdentity(
                                SessionId(
                                    session.id!!
                                ), data?.toString(),
                                sessTask,
                                workType = WorkType.Task,
                                profile = profile,
                                successful = true,
                                encryptContent = externalHandler.encryptAtRest()
                            ).onItem().invoke { _ -> sessTask.fulfilled = true }
                        } else internalHandler?.finishWorkSuccess(sessTask, data)
                            ?: throw UnconfiguredProfileException(session.profileId!!,
                                String.format(
                                    "Could not complete a task because the profile with id %s is missing",
                                    session.profileId
                                )
                            )
                        operation
                            .call { ->
                                if (!sessTask.fulfilled || !session.state!!.isChangedOnWorkFulfillment) {
                                    return@call Uni.createFrom().voidItem()
                                } else {
                                    return@call Mutiny.fetch(session.tasks)
                                        .chain { tasks ->
                                            var workType = sessTask.workType!!
                                            val runningTasks = if (workType == WorkType.Task) {
                                                profile.preconditionTasks
                                            } else {
                                                profile.preIssuanceActions
                                            }
                                            val calcTree = TaskTreeForCalculating.buildTree(
                                                runningTasks,
                                                tasks!!
                                            )
                                            if (calcTree.fulfilled()) {
                                                session.state = if (workType == WorkType.Task) {
                                                    NotarizationRequestState.SUBMITTABLE
                                                } else {
                                                    NotarizationRequestState.ACCEPTED
                                                }
                                                return@chain session.persist<Session>()
                                                    .replaceWithVoid()
                                            } else {
                                                return@chain Uni.createFrom().voidItem()
                                            }
                                        }
                                }
                            }
                    }
            }.replaceWithVoid()
    }

    private fun findHandlers(
        profile: Profile,
        sessionTask: SessionTask
    ): Pair<InternalServiceExtensionHandler?, ExternalServiceExtensionHandler?> {
        when(sessionTask.workType) {
            WorkType.Task -> {
                val taskDescription = profile.taskDescriptions.firstOrNull { it.name == sessionTask.name }
                    ?: throw InvalidTaskStateException("Could not find a task description for the task named ${sessionTask.name}")

                val internalHandler = internalServiceHandler.handleTask(taskDescription)
                val externalHander = externalServiceHandler.handleTask(taskDescription)
                return Pair(internalHandler, externalHander)
            }
            WorkType.Action -> {
                val issuanceAction = profile.actionDescriptions.firstOrNull { it.name == sessionTask.name }
                    ?: throw InvalidTaskStateException("Could not find a action description for the action named ${sessionTask.name}")

                val externalHander = externalServiceHandler.handleAction(issuanceAction)
                return Pair(null, externalHander)
            }
            else -> {
                throw NotImplementedError("Work type not supported: ${sessionTask.workType}")
            }
        }
    }

    @WithTransaction
    fun finishWorkSuccess(nonce: String, data: JsonNode?): Uni<Void> {
        return OngoingTask.find("nonce", nonce).firstResult()
            .onItem().ifNull().failWith { NotFoundException("Task") }
            .chain { t ->
                t!!.nonce = null
                t.persist<OngoingTask>()
                    .call { -> finishWorkSuccess(TaskId(t.taskId!!), data) }
            }.replaceWithVoid()
    }

    @WithTransaction
    fun finishWorkFail(taskId: TaskId, data: JsonNode?): Uni<Void?> {
        return SessionTask.findById(taskId.id)
            .onItem().ifNull().failWith { NotFoundException("TaskId") }
            .chain { sessTask: SessionTask? ->
                if (!sessTask!!.running) {
                    throw InvalidTaskStateException("Cannot finish not running task.")
                }
                sessTask.running = false
                val session = sessTask.session!!
                CallbackAuditingFilter.storeTaskName(request, sessTask.name)
                CallbackAuditingFilter.storeSessionId(request, session.id)

                val profileId = session.profileId!!
                profileService.find(profileId)
                    .chain { profile ->
                        if (profile == null) {
                            throw UnconfiguredProfileException(profileId, "Task refers to profile not available")
                        }
                        val (internalHandler, externalHandler) = findHandlers(profile, sessTask)

                        internalHandler?.finishWorkFail(sessTask, data)
                            ?: if (externalHandler != null) {
                                registry.counter("request.identity.failure").increment()
                                workService.assignIdentity(
                                    SessionId(
                                        session.id!!
                                    ), data?.toString(),
                                    sessTask,
                                    workType = WorkType.Task,
                                    profile = profile,
                                    successful = false,
                                    encryptContent = externalHandler.encryptAtRest()
                                ).onItem().invoke { _ -> sessTask.fulfilled = true }
                            } else {
                                throw UnconfiguredServiceException(profile.id, sessTask.name)
                            }
                    }
            }.replaceWithVoid()
    }

    @WithTransaction
    fun finishWorkFail(nonce: String, data: JsonNode?): Uni<Void> {
        return OngoingTask.find("nonce", nonce).firstResult()
            .onItem().ifNull().failWith { NotFoundException("Task") }
            .chain { t: OngoingTask? ->
                t!!.nonce = null
                t.persist<OngoingTask>()
                    .chain { _ -> finishWorkFail(TaskId(t.taskId!!), data) }
            }
    }

    private fun createNonce(): String {
        return urlSafeString(secureRandom, ByteArray(64))
    }

    private fun urlSafeString(secureRandom: SecureRandom, tokenBuffer: ByteArray): String {
        secureRandom.nextBytes(tokenBuffer)
        return Base64.getUrlEncoder().encodeToString(tokenBuffer)
    }

    private fun cancelRemote(cancelUri: String?): Uni<Void> {
        return if (cancelUri != null) {
            RestClientBuilder.newBuilder()
                .baseUri(URI.create(cancelUri))
                .build(CancelRemoteTaskApi::class.java)
                .cancel()
                .onFailure()
                .invoke { t -> logger.warn(t) { "The " } }
        } else {
            Uni.createFrom().voidItem()
        }
    }

}
