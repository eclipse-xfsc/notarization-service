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

import com.fasterxml.jackson.annotation.JsonValue
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
@Schema(
    description = "The filter types.",
    enumeration = [RequestFilter.Name.AVAILABLE, RequestFilter.Name.ALL_CLAIMED, RequestFilter.Name.OWN_CLAIMED]
)
enum class RequestFilter(private val value: String) {
    available(Name.AVAILABLE), allClaimed(Name.ALL_CLAIMED), ownClaimed(Name.OWN_CLAIMED);

    @JsonValue
    override fun toString(): String {
        return value
    }

    object Name {
        const val AVAILABLE = "available"
        const val ALL_CLAIMED = "allClaimed"
        const val OWN_CLAIMED = "ownClaimed"
    }
}
