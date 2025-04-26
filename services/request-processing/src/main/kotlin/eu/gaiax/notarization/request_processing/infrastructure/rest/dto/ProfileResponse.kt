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

import eu.gaiax.notarization.request_processing.domain.model.CredentialType
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI
import java.util.*

/**
 *
 * @author Neil Crossley
 */
class ProfileResponse {
    @NotNull var id: UUID? = null
    @NotEmpty var name: String? = null

    @get:Schema(type = SchemaType.STRING, format = "uri", nullable = false)
    @NotEmpty var schema: URI? = null
    @NotNull var credential: CredentialType? = null
}
