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
package eu.gaiax.notarization.api.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.util.stream.Stream

/**
 *
 * @author Florian Otto
 */
@Schema(
    implementation = ProfileTaskTree.TaskTreeNode::class,
    hidden = true,
)
class ProfileTaskTree(taskName: String?) {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    var allOf: Set<ProfileTaskTree> = HashSet()

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    var oneOf: Set<ProfileTaskTree> = HashSet()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var taskName: String? = null
    init {
        this.taskName = taskName
    }
    constructor() : this(null)

    private fun treeEmpty(): Boolean {
        return (taskName == null && allOf.isEmpty()
                && oneOf.isEmpty())
    }

    @get:JsonIgnore
    val isEmpty: Boolean
        get() = (taskName == null && allOf.isEmpty()
            && oneOf.isEmpty())

    fun containsTaskByName(taskName: String): Boolean {
        return if (this.taskName == taskName) {
            true
        } else {
            Stream.concat(allOf.stream(), oneOf.stream())
                .anyMatch { childTree: ProfileTaskTree -> childTree.containsTaskByName(taskName) }
        }
    }

    fun allNames(): Set<String> {
        val res = mutableSetOf<String>()
        fillNames(res)
        return res
    }

    private fun fillNames(set: MutableSet<String>) {
        val currentName = taskName
        if (currentName != null) {
            set.add(currentName)
        } else {
            allOf.forEach { it.fillNames(set) }
            oneOf.forEach { it.fillNames(set) }
        }
    }

    fun structureValid(): Boolean {
        return if (taskName != null) {
            allOf.isEmpty() && oneOf.isEmpty()
        } else if (allOf.isNotEmpty()) {
            (oneOf.isEmpty()
                    && allOf.stream().allMatch { sub: ProfileTaskTree -> sub.structureValid() })
        } else {
            oneOf.stream().allMatch { sub: ProfileTaskTree -> sub.structureValid() }
        }
    }

    //tells if a nonEmpty tree is fulfilled without the need of performing any task (all optional)
    fun fulfilledByDefault(): Boolean {
        //no tasks, that's ok
        return if (allNames().isEmpty()) {
            false
        } else {
            fullfilled()
        }
    }

    private fun fullfilled(): Boolean {
        if (treeEmpty()) {
            return true
        } else if (allOf.isNotEmpty()) {
            return allOf.stream().allMatch { subtree: ProfileTaskTree -> subtree.fullfilled() }
        } else if (oneOf.isNotEmpty()) {
            return oneOf.stream().anyMatch { subtree: ProfileTaskTree -> subtree.fullfilled() }
        }
        return false
    }

    @Schema(
        oneOf = [NodeEmpty::class, NodeAllOf::class, NodeOneOf::class, NodeTask::class])
    class TaskTreeNode

    @Schema(
        description = "This represents an empty node. There is no work to be done.",
        example = """
                      {  }
                      """
    )
    class NodeEmpty

    @Schema(
        description = "This represents a node satisfied when any single child node is fulfilled.",
        example = """
                      { "oneOf": [
                          { "taskName": "vcIdentification" },
                          { "taskName": "eID" }
                      ] }
                      """)
    class NodeOneOf {
        @get:Schema(
            description = "The array of child nodes.",
            required = true)
        var oneOf: List<TaskTreeNode>? = null
    }

    @Schema(
        required = true,
        description = "This represents a node satisfied only when every single child node is fulfilled.",
        example = """
                    { "allOf": [
                        { "taskName": "eID" },
                        { "taskName": "signedFormUpload" }
                    ] }
                  """)
    class NodeAllOf {
        @get:Schema(
            description = "The array of child nodes.",
            required = true)
        var allOf: List<TaskTreeNode>? = null
    }

    @Schema(
        description = "This represents a node satisfied when the named work (a task or actions) is fulfilled.",
        example = """
                  { "taskName": "eID" }
                  """)
    class NodeTask {
        @get:Schema(
            description = "The name of the work to be performed and satisfied.",
            required = true)
        var taskName: String? = null
    }
}
