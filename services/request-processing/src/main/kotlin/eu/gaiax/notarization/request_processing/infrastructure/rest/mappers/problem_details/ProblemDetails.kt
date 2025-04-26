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
package eu.gaiax.notarization.request_processing.infrastructure.rest.mappers.problem_details

import io.quarkus.logging.Log
import jakarta.validation.constraints.NotNull
import java.net.URI
import java.net.URISyntaxException

open class ProblemDetails(
    @NotNull val type: URI?,
    @NotNull val title: String?,
    @NotNull val status: Int,
    @NotNull val detail: String?,
    val instance: String?
) {
    constructor(title: String?, status: Int, detail: String?, instance: String?) : this(
        ABOUT_BLANK,
        title,
        status,
        detail,
        instance
    ) {
    }

    companion object {
        var ABOUT_BLANK: URI? = null

        init {
            var aboutBlank: URI? = try {
                URI("about:blank")
            } catch (e: URISyntaxException) {
                Log.error("Could not create the 'about:blank' URI!", e)
                null
            }
            ABOUT_BLANK = aboutBlank
        }
    }
}
