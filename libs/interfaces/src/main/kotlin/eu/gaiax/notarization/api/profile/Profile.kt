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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.Period


/**
 *
 * @author Neil Crossley
 */
@Schema(description = "A notarization profile specifies a single notarization submission process and the resulting credential type.")
data class Profile(
    @get:Schema(required = true, description = "The unique identifier of the profile.")
    @NotNull val id: String,
    @Deprecated(message = "Use kind instead", replaceWith = ReplaceWith("kind"))
    @get:Schema(deprecated = true)
    val aip: AipVersion?,
    @NotNull val kind: CredentialKind?,
    @get:Schema(required = true, description = "A human readable name of the profile")
    @NotNull val name: String,
    @get:Schema(required = true, description = "A human readable description of the profile")
    @NotNull val description: String,
    @get:Schema(required = false, description = "A range of OIDC roles required to access notarization requests related for this profile via REST. A single role is sufficient to grant access.")
    @NotNull val notaryRoles: Set<String>,
    @get:Schema(required = false, description = "This value specifies the fallback encryption algorithm if not defined in a NotaryAccess.")
    @NotNull val encryption: String,
    @NotNull val notaries: List<NotaryAccess>,
    @NotNull val validFor: Period?,
    @NotNull val isRevocable: Boolean,
    @get:Schema(required = true,
        description = "This value is the should contain the fixed content of the VC to be issued.",
        implementation = Any::class
    )
    @NotNull val template: JsonNode,
    @get:Schema(description = "A template that is applied to the evidence documents. The result is merged into the issued credential. The specification is StringTemplate (https://www.stringtemplate.org/).")
    val documentTemplate: String?,
    @NotNull val taskDescriptions: List<TaskDescription>,
    @get:Schema(required = true,
        description = "The structured tasks the requestor must satisfy before completing the notarization request.",
        ref = "#/components/schemas/TaskTreeNode",
        implementation = ProfileTaskTree.TaskTreeNode::class,
        example = "{ \"anyOf\": [ { \"taskName\": \"signedUpload\" }, { } ] }")
    @NotNull val tasks: ProfileTaskTree,
    @get:Schema(required = true,
        implementation = ProfileTaskTree.TaskTreeNode::class,
        ref = "#/components/schemas/TaskTreeNode",
        example = "{ \"allOf\": [ { \"taskName\": \"eID\" }, { \"taskName\": \"signedFormUpload\" } ] } ")
    @NotNull val preconditionTasks: ProfileTaskTree,
    @get:Schema(required = true,
        implementation = ProfileTaskTree.TaskTreeNode::class,
        ref = "#/components/schemas/TaskTreeNode",
        example = """{ "allOf": [ { "taskName": "validate-TRAIN" } ] }""")
    @NotNull val preIssuanceActions: ProfileTaskTree,
    @NotNull val postIssuanceActions: List<String>,
    @NotNull val actionDescriptions: List<IssuanceAction>,
    @get:Schema(implementation = Any::class)
    val extensions: Map<String, JsonNode> = emptyMap()
) {

}
