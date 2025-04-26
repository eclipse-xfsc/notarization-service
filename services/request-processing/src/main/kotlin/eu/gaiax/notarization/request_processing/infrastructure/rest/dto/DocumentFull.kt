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
import eu.gaiax.notarization.request_processing.domain.model.VerificationReport
import eu.gaiax.notarization.request_processing.domain.model.VerificationReport.Companion.valueOf
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
class DocumentFull : DocumentView() {
    @get:Schema(required = true)
    var content: ByteArray? = null

    @get:Schema(required = true)
    var verificationReport: VerificationReport? = null

    @get:Schema(required = true)
    var hash: String? = null

    companion object {
        fun fromDocument(document: Document): DocumentFull {
            val full = DocumentFull()
            full.id = DocumentId(document.id!!)
            full.content = document.content
            full.verificationReport = valueOf(document.verificationReport)
            full.hash = document.hash
            full.title = document.title
            full.shortDescription = document.shortDescription
            full.longDescription = document.longDescription
            full.mimetype = document.mimetype
            full.extension = document.extension
            return full
        }
    }
}
