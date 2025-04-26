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
package eu.gaiax.notarization.request_processing.infrastructure.rest.resource

import eu.gaiax.notarization.request_processing.application.DocumentStore
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import eu.gaiax.notarization.request_processing.infrastructure.rest.resource.NotarizationRequestSubmissionResource.AccessPathParameters
import io.micrometer.core.instrument.MeterRegistry
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.PermitAll
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.eclipse.microprofile.openapi.annotations.tags.Tags
import org.jboss.resteasy.reactive.*
import java.util.*

/**
 *
 * @author Florian Otto
 */
@Tags(Tag(name = Api.Tags.SUBMISSION), Tag(name = Api.Tags.DOCUMENT))
@Path(Api.Path.DOCUMENT_RESOURCE)
@PermitAll
@SecurityRequirements
class DocumentResource(
    private val registry: MeterRegistry,
    private val documentStore: DocumentStore
) {
    @Auditable(action = NotarizationRequestAction.UPLOAD_DOCUMENT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(
        DOCUMENTS_PATH + "/" + Api.Param.TASKID_PARAM + Api.Path.DOCUP_BYLINK
    )
    @ResponseStatus(204)
    @Operation(
        summary = "Upload document by providing link",
        description = "Upload a document for the identified notarization request by providing a link to it."
    )
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    fun uploadDocumentByLink(
        @BeanParam @Valid params: AccessPathParameters,
        @PathParam(Api.Param.TASKID) taskId: TaskId,
        @Valid document: DocumentUploadByLink
    ): Uni<Void> {
        registry.counter("request.submission.upload-document-by-link.call").increment()
        return documentStore.uploadByLink(document, taskId, params)
    }

    @Auditable(action = NotarizationRequestAction.UPLOAD_DOCUMENT)
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path(
        DOCUMENTS_PATH + "/" + Api.Param.TASKID_PARAM + Api.Path.DOCUP_CONTENT
    )
    @ResponseStatus(204)
    @Operation(summary = "Upload document", description = "Upload a document for the identified notarization request.")
    @Throws(
        InvalidRequestStateException::class,
        NotFoundException::class,
        AuthorizationException::class,
        InvalidFileException::class
    )
    fun uploadDocument(
        @BeanParam @Valid params: AccessPathParameters,
        @PathParam(Api.Param.TASKID) taskId: TaskId,
        @Valid document: DocumentUpload
    ): Uni<Void> {
        registry.counter("request.submission.upload-document-directly.call").increment()
        return documentStore.upload(document, taskId, params)
    }

    @Auditable(action = NotarizationRequestAction.FETCH_DOCUMENT)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Path(
        DOCUMENTS_PATH + "/" + Api.Param.NOTARIZATION_REQUEST_DOCUMENT_ID_PARAM
    )
    @Operation(summary = "Fetch document", description = "Gets the document description for the identified document.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    operator fun get(
        @BeanParam @Valid params: AccessPathParameters,
        @PathParam(Api.Param.DOCUMENTID) documentId: UUID
    ): Uni<DocumentView> {
        registry.counter("request.submission.documents.get.call").increment()
        return documentStore[documentId, params]
    }

    @Auditable(action = NotarizationRequestAction.DELETE_DOCUMENT)
    @DELETE
    @Path(DOCUMENTS_PATH + "/" + Api.Param.NOTARIZATION_REQUEST_DOCUMENT_ID_PARAM)
    @ResponseStatus(204)
    @Operation(summary = "Delete document", description = "Delete the identified document.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    fun delete(
        @BeanParam @Valid params: AccessPathParameters,
        @PathParam(Api.Param.DOCUMENTID) documentId: UUID
    ): Uni<Void> {
        registry.counter("request.submission.documents.delete.call").increment()
        return documentStore.delete(documentId, params)
    }

    @Auditable(action = NotarizationRequestAction.TASK_FINISH_SUCCESS)
    @ResponseStatus(204)
    @POST
    @Path(DOCUMENTS_PATH + "/" + Api.Param.TASKID_PARAM + "/" + Api.Path.FINISH_TASK)
    @Operation(summary = "Finish uploading", description = "Mark uploading of documents as finished.")
    fun finishUploading(
        @BeanParam @Valid params: AccessPathParameters,
        @PathParam(Api.Param.TASKID) taskId: TaskId
    ): Uni<Void> {
        return documentStore.finish(taskId, params)
    }

    companion object {
        const val DOCUMENTS_PATH = "/" + Api.Param.SESSION_PARAM
    }
}
