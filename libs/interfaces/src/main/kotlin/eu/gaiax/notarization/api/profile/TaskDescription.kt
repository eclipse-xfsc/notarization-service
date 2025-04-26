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

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI

class TrainParameter (
    var trustSchemePointers: List<String> = listOf(),
    var endpointTypes: List<String> = listOf(),
)

/**
 *
 * @author Florian Otto
 */
@Schema(
    description = "A description for a task that is performed before or during the submission process, by the requester.",
    example =
    """
            {
                "name": "train-enrollment",
                "serviceName": "train-enrollment-proxy"
            }
        """,
    additionalProperties = Schema.True::class,
)
class TaskDescription (
    name: String,
    val type: TaskType?,
    description: String? = null,
    serviceName: String? = null,
    serviceLocation: URI? = null,
    encryptAtRest: Boolean? = null,
    @get:JsonAnyGetter
    @field:JsonAnySetter
    @field:Schema(hidden = true)
    val extensions: MutableMap<String, JsonNode> = mutableMapOf(),
): WorkDescription(name = name, description = description, serviceName = serviceName, serviceLocation = serviceLocation, encryptAtRest = encryptAtRest)
