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
package eu.gaiax.notarization.request_processing.domain.model.taskprocessing

import io.quarkus.runtime.annotations.RegisterForReflection
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI

/**
 *
 * @author Florian Otto
 */
@RegisterForReflection
@Schema(type = SchemaType.STRING)
class TaskInstance {
    var taskId: TaskId? = null
    var uri: URI? = null
    var taskName: String? = null

    constructor() {}
    constructor(taskId: TaskId, uri: URI?) {
        this.taskId = taskId
        this.uri = uri
    }
    constructor(taskId: TaskId, uri: URI?, taskName: String?) {
        this.taskId = taskId
        this.uri = uri
        this.taskName = taskName
    }
}
