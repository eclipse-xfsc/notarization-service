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
package eu.gaiax.notarization.request_processing.infrastructure.rest.dto

import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import eu.gaiax.notarization.request_processing.domain.model.*
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.util.function.Function
import java.util.stream.Collectors

/**
 *
 * @author Neil Crossley
 */
class SessionSummary(
    @get:Schema(readOnly = true) val sessionId: SessionId,
    @get:Schema(readOnly = true) val profileId: String,
    @get:Schema(readOnly = true) val state: NotarizationRequestState,
    val tasks: Set<SessionTaskSummary>,
    val preconditionTaskTree: SessionTaskTree,
    val taskTree: SessionTaskTree,
    val preconditionTasksFulfilled: Boolean,
    val tasksFulfilled: Boolean

) {
    companion object {
        fun asSummary(
            dbSession: Session,
            p: Profile
        ): SessionSummary {
            val notaryTasks = dbSession.tasks!!.filter { it.workType == WorkType.Task }
            return SessionSummary(
                SessionId(dbSession.id!!),
                p.id,
                dbSession.state!!,
                notaryTasks.map { SessionTaskSummary.valueOf(
                    it
                ) }.toSet(),
                SessionTaskTree.buildTree(p.preconditionTasks, notaryTasks),
                SessionTaskTree.buildTree(p.tasks, notaryTasks),
                p.preconditionTasks.treeFulfilledBySession(dbSession),
                p.tasks.treeFulfilledBySession(dbSession)
            )
        }
    }
}
