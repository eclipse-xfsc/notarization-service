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
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.jboss.resteasy.reactive.PartType
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload

/**
 *
 * @author Neil Crossley
 */
class DocumentUpload {
    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    @NotNull var id: DocumentId? = null

    @RestForm("content")
    @get:Schema(type = SchemaType.STRING, format = "binary", description = "An evidence document")
    @NotNull var content: FileUpload? = null

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    var title: String? = null

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    var shortDescription: String? = null

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    var longDescription: String? = null
}
