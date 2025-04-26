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

import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import java.util.stream.Collectors

/**
 *
 * @author Florian Otto
 */


fun ProfileTaskTree.treeFulfilledBySession(s: Session): Boolean {
    val calcTree: TaskTreeForCalculating = TaskTreeForCalculating.buildTree(
        this,
        s.tasks?.associateBy { it.name!! } ?: mapOf()
    )
    return calcTree.fulfilled()
}

class TaskTreeForCalculating {
    var fulfilled = false
    var allOf: Set<TaskTreeForCalculating> = setOf()
    var oneOf: Set<TaskTreeForCalculating> = setOf()

    fun fulfilled(): Boolean {
        if (fulfilled) {
            return true
        } else if (allOf.isNotEmpty()) {
            return allOf.all { t -> t.fulfilled() }
        } else if (oneOf.isNotEmpty()) {
            return oneOf.any { t -> t.fulfilled() }
        }
        return false
    }

    companion object {

        fun buildTree(profileTree: ProfileTaskTree, tasks: Set<SessionTask>): TaskTreeForCalculating {
            val calcTree = TaskTreeForCalculating()
            if (profileTree.isEmpty) {
                calcTree.fulfilled = true
            } else {
                if (profileTree.taskName != null) {
                    calcTree.fulfilled = tasks.any { it.name == profileTree.taskName && it.fulfilled }
                } else {
                    calcTree.allOf = profileTree.allOf.map { buildTree(it, tasks) }.toSet()
                    calcTree.oneOf = profileTree.oneOf.map { buildTree(it, tasks) }.toSet()
                }
            }
            return calcTree
        }
        fun buildTree(profileTree: ProfileTaskTree, tasks: Map<String, SessionTask>): TaskTreeForCalculating {
            val calcTree = TaskTreeForCalculating()
            if (profileTree.isEmpty) {
                calcTree.fulfilled = true
            } else {
                val taskName = profileTree.taskName
                if (taskName != null) {
                    val foundTask = tasks[taskName]
                    calcTree.fulfilled = foundTask?.fulfilled ?: false
                } else {
                    calcTree.allOf = profileTree.allOf.map { buildTree(it, tasks) }.toSet()
                    calcTree.oneOf = profileTree.oneOf.map { buildTree(it, tasks) }.toSet()
                }
            }
            return calcTree
        }
    }
}
