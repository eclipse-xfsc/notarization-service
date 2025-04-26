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

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.request_processing.domain.model.DistributedIdentity
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
class SubmitNotarizationRequest {

    @get:Schema(
        nullable = false,
        description = "This value is the requested payload of the credentials to be issued by the notarization system."
    )
    @NotNull var data: JsonNode? = null
    var holder: DistributedIdentity? = null
    var invitation: String? = null
}
