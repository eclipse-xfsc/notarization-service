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
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.exception.BusinessException
import eu.gaiax.notarization.request_processing.domain.model.DistributedIdentity
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.*
import java.util.stream.Collectors

/**
 *
 * @author Neil Crossley
 */
@Schema(description = "The overview of the notarization request")
class NotarizationRequestView {
    @get:Schema(readOnly = true)
    @NotNull var id: NotarizationRequestId? = null

    @get:Schema(readOnly = true)
    @get:NotNull var profileId: ProfileId? = null

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

    @get:Schema(description = "The target holder of the credentials to be issued.")
    var holder: DistributedIdentity? = null

    @get:Schema(description = "Summary of uploaded evidence documents.")
    var documents: Set<DocumentView>? = null

    @get:Schema(description = "The notarization request may be rejected by a notary. This is the given reason.")
    var rejectComment: String? = null

    @get:Schema(
        description = "The structure assigned by the notary."
                + " This structure will be merged into the issued credential when the template in the profile is applied."
                + " This may extend or override the structure of the credential."
    )
    var credentialAugmentation: ObjectNode? = null

    companion object {
        @Throws(BusinessException::class)
        fun from(request: NotarizationRequest, objectMapper: ObjectMapper): NotarizationRequestView {
            return try {
                val view = NotarizationRequestView()
                view.id = NotarizationRequestId(request.id!!)
                view.profileId = request.session!!.profileId!!
                view.createdAt = request.createdAt
                view.lastModified = request.lastModified
                view.requestState = request.session!!.state
                view.data = objectMapper.readTree(request.data)
                view.holder = DistributedIdentity(request.did)
                view.credentialAugmentation = Optional.ofNullable(request.credentialAugmentation).map { v: String? ->
                    try {
                        return@map objectMapper.readTree(v) as ObjectNode
                    } catch (e: JsonProcessingException) {
                        throw IllegalStateException("Invalid json data", e)
                    }
                }.orElse(null)
                view.documents = request.session!!.documents!!.stream()
                    .map<DocumentView> { document: Document -> DocumentView.Companion.fromDocument(document) }
                    .collect(Collectors.toSet<DocumentView>())
                view.rejectComment = request.rejectComment
                view
            } catch (e: JsonProcessingException) {
                throw BusinessException("Invalid json data", e)
            }
        }
    }
}
