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
package eu.gaiax.notarization.request_processing.domain.model

import com.fasterxml.jackson.annotation.JsonInclude
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SessionTaskSummary
import java.util.stream.Collectors

/**
 *
 * @author Florian Otto
 */
class SessionTaskTree {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var task: SessionTaskSummary? = null

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    var allOf: Set<SessionTaskTree> = mutableSetOf()

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    var oneOf: Set<SessionTaskTree> = mutableSetOf()

    companion object {
        fun buildTree(profileTree: ProfileTaskTree, tasks: List<SessionTask>): SessionTaskTree {
            val sessTaskTree = SessionTaskTree()
            if (profileTree.taskName != null) {
                sessTaskTree.task = tasks
                    .firstOrNull { t -> t.name == profileTree.taskName }
                    ?.let { SessionTaskSummary.valueOf(it) }
            } else {
                sessTaskTree.allOf = profileTree.allOf.map { buildTree(it, tasks) }.toSet()
                sessTaskTree.oneOf = profileTree.oneOf.map { buildTree(it, tasks) }.toSet()
            }
            return sessTaskTree
        }
    }
}
