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
package eu.gaiax.notarization.request_processing.domain.entity

import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.gaiax.notarization.api.profile.WorkDescription
import eu.gaiax.notarization.request_processing.application.taskprocessing.WorkExecutionEngine
import eu.gaiax.notarization.request_processing.domain.exception.InvalidProfileException
import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.domain.services.RequestNotificationService
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.smallrye.mutiny.Uni
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.reactive.mutiny.Mutiny
import java.time.OffsetDateTime
import java.util.*
import java.util.function.Supplier

/**
 *
 * @author Neil Crossley
 */
@Entity
@Table(name = "requestsession")
class Session : PanacheEntityBase {

    @Id
    var id: String? = null

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "session", cascade = [CascadeType.ALL])
    var request: NotarizationRequest? = null

    var accessToken: String? = null

    var profileId: ProfileId? = null
    var identityToken: String? = null

    var manualRelease: Boolean? = null

    var manualReleaseToken: String? = null

    var state: NotarizationRequestState? = null

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "session")
    var identities: MutableSet<RequestorIdentity>? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    var lastModified: OffsetDateTime? = null

    @Version
    var version: OffsetDateTime? = null

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "session")
    var tasks: MutableSet<SessionTask>? = null

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var documents: MutableSet<Document>? = null

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "session")
    var issuanceProcesses: MutableList<IssuanceProcess>? = null

    fun loadIdentities(): Uni<Session> {
        return Mutiny.fetch(identities).onItem().transform { this }
    }

    @WithTransaction
    fun cleanup(workEngine: WorkExecutionEngine, notifications: RequestNotificationService): Uni<Void> {
        manualReleaseToken = null
        return this.persistAndFlush<Session>()
            .chain { _ ->
                workEngine.cancelWork(this)
                    .onFailure().recoverWithNull()
            }
            .chain { _ -> Document.delete("session", this) }
            .chain { _ -> IssuanceProcess.delete("session", this) }
            .chain { _ -> RequestorIdentity.delete("session", this) }
            .chain { _ -> NotarizationRequest.delete("session", this) }
            .invoke { _ ->
                notifications.onDeleted(
                    SessionId(
                        id!!
                    )
                )
            }
            .replaceWithVoid()
    }

    companion object : PanacheCompanionBase<Session, String> {
        @JvmStatic
        fun createNew(id: SessionId, token: AccessToken, profile: Profile): Uni<Session> {
            val newSession = Session()
            newSession.id = id.id
            newSession.accessToken = token.token
            newSession.profileId = ProfileId(profile.id)
            newSession.manualRelease = false
            newSession.tasks = HashSet()
            val persistTasks = buildSessionTasks(newSession, profile)
            return if (persistTasks.isEmpty()) {
                newSession.persistAndFlush()
            } else {
                Uni.combine().all().unis<Any>(persistTasks).discardItems()
                    .chain(Supplier { newSession.persistAndFlush() })
            }
        }

        private fun buildSessionTasks(newSession: Session, profile: Profile): List<Uni<Void>> {
            val preConTasks = profile.preconditionTasks
            newSession.state =
                if (preConTasks.treeFulfilledBySession(newSession)) NotarizationRequestState.SUBMITTABLE else NotarizationRequestState.CREATED
            val taskDescriptionsByName = profile.taskDescriptions.associateBy { it.name }
            val taskNames = mutableSetOf<String>()
            extractTaskNames(preConTasks, taskNames)
            extractTaskNames(profile.tasks, taskNames)
            val sessiontasks = asPersistOperations(taskNames, taskDescriptionsByName, profile, newSession, WorkType.Task)

            val actionDescriptionsByName = profile.actionDescriptions.associateBy { it.name }
            val actionNames = profile.postIssuanceActions.toMutableSet()
            extractTaskNames(profile.preIssuanceActions, actionNames)
            val actionTasks = asPersistOperations(actionNames, actionDescriptionsByName, profile, newSession, WorkType.Action)

            return sessiontasks + actionTasks
        }

        private fun asPersistOperations(
            taskNames: Set<String>,
            taskDescriptionsByName: Map<String, WorkDescription>,
            profile: Profile,
            newSession: Session,
            workType: WorkType
        ): List<Uni<Void>> {
            return taskNames
                .map { e ->
                    val desc = taskDescriptionsByName[e] ?: throw InvalidProfileException(ProfileId(profile.id))
                    val sessTask = SessionTask()
                    sessTask.taskId = UUID.randomUUID()
                    sessTask.fulfilled = false
                    sessTask.running = false
                    sessTask.name = desc.name
                    sessTask.workType = workType
                    sessTask.session = newSession
                    newSession.tasks!!.add(sessTask)
                    sessTask.persist<SessionTask>().replaceWithVoid()
                }
        }
        private fun extractTaskNames(tree: ProfileTaskTree, names: MutableSet<String>) {
            val taskName = tree.taskName
            if (taskName != null) {
                names.add(taskName)
            } else {
                tree.allOf.stream().forEach { t: ProfileTaskTree -> extractTaskNames(t, names) }
                tree.oneOf.stream().forEach { t: ProfileTaskTree -> extractTaskNames(t, names) }
            }
        }

        fun findWithIdentities(sessionId: SessionId): Uni<Session?> {
            return Session.findById(sessionId.id)
                .call { session: Session? ->
                    if (session == null) {
                        Uni.createFrom().nullItem<Session>()
                    } else {
                        Mutiny.fetch(session.identities)
                    }
                }
        }

        @JvmStatic
        fun findWithDocuments(sessionId: SessionId): Uni<NotarizationRequest> {
            return Session.findById(sessionId.id)
                .chain { session: Session? ->
                    if (session == null) {
                        throw NotFoundException("Session")
                    } else {
                        Mutiny.fetch(session.documents)
                            .chain { _ -> Mutiny.fetch(session.request) }
                    }
                }
        }
    }
}
