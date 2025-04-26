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

import eu.gaiax.notarization.request_processing.domain.entity.Document
import eu.gaiax.notarization.request_processing.domain.model.DocumentId
import jakarta.validation.constraints.NotNull

/**
 *
 * @author Neil Crossley
 */
open class DocumentView {
    @NotNull var id: DocumentId? = null
    @NotNull var title: String? = null
    var shortDescription: String? = null
    var longDescription: String? = null
    var mimetype: String? = null
    var extension: String? = null

    companion object {
        fun fromDocument(document: Document): DocumentView {
            val view = DocumentView()
            view.id = DocumentId(document.id!!)
            view.title = document.title
            view.shortDescription = document.shortDescription
            view.longDescription = document.longDescription
            view.mimetype = document.mimetype
            view.extension = document.extension
            return view
        }
    }
}
