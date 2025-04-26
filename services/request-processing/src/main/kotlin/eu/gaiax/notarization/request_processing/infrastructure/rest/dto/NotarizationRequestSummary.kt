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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequestDbView
import eu.gaiax.notarization.request_processing.domain.exception.BusinessException
import eu.gaiax.notarization.request_processing.domain.model.DistributedIdentity
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.OffsetDateTime

/**
 *
 * @author Neil Crossley
 */
@Schema(description = "The overview of the notarization request")
class NotarizationRequestSummary {
    @NotNull var id: NotarizationRequestId? = null
    @NotNull var profileId: ProfileId? = null

    @get:Schema(readOnly = true)
    @NotNull var createdAt: OffsetDateTime? = null

    @get:Schema(readOnly = true)
    @NotNull var lastModified: OffsetDateTime? = null

    @get:Schema(readOnly = true, description = "The current processed state of the notarization request.")
    @NotNull var requestState: NotarizationRequestState? = null

    @get:Schema(
        type = SchemaType.OBJECT,
        nullable = false,
        description = "The proposed content of the credential to be issued."
    )
    @NotNull var data: JsonNode? = null
    var holder: DistributedIdentity? = null
    var totalDocuments = 0

    @get:Schema(description = "The notarization request may be rejected by a notary. This is the given reason.")
    var rejectComment: String? = null

    companion object {
        @Throws(BusinessException::class)
        fun from(req: NotarizationRequestDbView, objectMapper: ObjectMapper): NotarizationRequestSummary {
            return try {
                val summary = NotarizationRequestSummary()
                summary.id = NotarizationRequestId(req.requestId!!)
                summary.createdAt = req.createdAt
                summary.profileId = ProfileId(req.profileId!!)
                summary.lastModified = req.lastModified
                summary.requestState = req.state
                val data = req.data
                summary.data = if (data != null) objectMapper.readTree(req.data) else null
                summary.holder = DistributedIdentity(req.did)
                val totalDocuments = req.totalDocuments
                summary.totalDocuments = totalDocuments ?: 0
                summary.rejectComment = req.rejectComment
                summary
            } catch (e: JsonProcessingException) {
                throw BusinessException("Invalid json data", e)
            }
        }
    }
}
