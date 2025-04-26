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

import eu.gaiax.notarization.request_processing.domain.model.DocumentId
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI

/**
 *
 * @author Neil Crossley
 */
class DocumentUploadByLink {
    @NotNull var id: DocumentId? = null

    @get:Schema(type = SchemaType.STRING, format = "uri")
    @NotNull var location: URI? = null
    var title: String? = null
    var mimetype: String? = null
    var extension: String? = null
    var shortDescription: String? = null
    var longDescription: String? = null
}
