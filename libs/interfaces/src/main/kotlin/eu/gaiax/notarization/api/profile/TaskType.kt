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

import com.fasterxml.jackson.annotation.JsonValue
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Florian Otto
 */
@Schema(
    description = "The task types.",
    enumeration = [TaskType.Name.BROWSER_IDENTIFICATION_TASK, TaskType.Name.FILEPROVISION_TASK, TaskType.Name.VC_IDENTIFICATION_TASK]
)
@Deprecated("Tasks are no longer identified by type.")
enum class TaskType(private val value: String) {
    BROWSER_IDENTIFICATION_TASK(Name.BROWSER_IDENTIFICATION_TASK), FILEPROVISION_TASK(Name.FILEPROVISION_TASK), VC_IDENTIFICATION_TASK(
        Name.VC_IDENTIFICATION_TASK
    );

    @JsonValue
    override fun toString(): String {
        return value
    }

    object Name {
        const val BROWSER_IDENTIFICATION_TASK = "browserIdentificationTask"
        const val FILEPROVISION_TASK = "fileProvisionTask"
        const val VC_IDENTIFICATION_TASK = "vcIdentificationTask"
    }

    companion object {
        fun fromString(s: String): TaskType {
            return if (s == Name.BROWSER_IDENTIFICATION_TASK) {
                BROWSER_IDENTIFICATION_TASK
            } else if (s == Name.FILEPROVISION_TASK) {
                FILEPROVISION_TASK
            } else if (s == Name.VC_IDENTIFICATION_TASK) {
                VC_IDENTIFICATION_TASK
            } else VC_IDENTIFICATION_TASK
        }
    }
}
