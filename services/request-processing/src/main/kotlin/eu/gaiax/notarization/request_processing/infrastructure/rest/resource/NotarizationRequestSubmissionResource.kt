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

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.request_processing.application.NotarizationRequestStore
import eu.gaiax.notarization.request_processing.application.WorkService
import eu.gaiax.notarization.request_processing.application.domain.SessionInfo
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskInstance
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SessionInfoResponse.Companion.valueOf
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SubmissionResponse.Companion.from
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import io.micrometer.core.instrument.MeterRegistry
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.PermitAll
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.reactive.*
import java.net.URI
import java.util.*

@Tag(name = Api.Tags.SUBMISSION)
@Path(Api.Path.SESSION_RESOURCE)
@PermitAll
@SecurityRequirements
class NotarizationRequestSubmissionResource(
    private val requestStore: NotarizationRequestStore,
    private val workService: WorkService,
    private val registry: MeterRegistry
) {
    @Auditable(action = NotarizationRequestAction.CREATE_SESSION)
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(
        MediaType.APPLICATION_JSON
    )
    @Operation(
        summary = "Create a new session",
        description = "Creates a new session for submitting a new notarization request."
    )
    @ResponseStatus(201)
    @APIResponse(
        responseCode = "201",
        description = "Details of the new session.",
        content = [Content(schema = Schema(implementation = SessionInfoResponse::class))]
    )
    fun create(
        @Context request: UriInfo,
        @NotNull @Valid body: SessionSubmission
    ): Uni<RestResponse<SessionInfoResponse>> {
        registry.counter("request.submission.create-token.call").increment()
        return requestStore.createNewSession(body.profileId!!)
            .map { session: Session ->
                val response = RestResponse.ResponseBuilder.created<SessionInfoResponse>(
                    request.baseUriBuilder.path(Api.Path.SESSION_RESOURCE).path(SESSION_PATH).build(session.id)
                )
                    .entity(valueOf(session)) // HACK: align this operation with the other auditable requestor operations
                    .header(Api.Param.SESSION, session.id)
                    .build()
                response
            }
    }

    @Auditable(action = NotarizationRequestAction.REVOKE)
    @DELETE
    @Path(REQUEST_PATH)
    @Operation(summary = "Revoke notarization request", description = "Revokes the identified notarization request.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The notarization request was successfully revoked.")
    @Throws(
        AuthorizationException::class, InvalidRequestStateException::class
    )
    fun delete(
        @BeanParam @Valid pathParams: AccessPathParameters
    ): Uni<Void> {
        registry.counter("request.submission.revoke-request.call").increment()
        return requestStore.softDeleteSession(pathParams.asSessionInfo())
    }

    @GET
    @Path(SESSION_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Fetch the session", description = "Fetches the identified session.")
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A summary of the session.",
        content = [Content(schema = Schema(implementation = SessionSummary::class))]
    )
    @Throws(
        AuthorizationException::class, NotFoundException::class, InvalidRequestStateException::class
    )
    fun fetchSession(
        @Context request: UriInfo,
        @BeanParam @Valid pathParams: AccessPathParameters
    ): Uni<SessionSummary> {
        registry.counter("request.submission.session.call").increment()
        return requestStore.fetchSession(pathParams.asSessionInfo())
    }

    @Auditable(action = NotarizationRequestAction.UPDATE_CONTACT)
    @PUT
    @Path("$SESSION_PATH/updateContact")
    @Consumes(
        MediaType.TEXT_PLAIN
    )
    @Operation(summary = "Update contact", description = "Updates contact information for receiving notifications.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The notarization request was successfully updated.")
    @Throws(
        AuthorizationException::class, NotFoundException::class, InvalidRequestStateException::class
    )
    fun updateContact(
        @Context request: UriInfo,
        @BeanParam @Valid pathParams: AccessPathParameters,
        contact: String?
    ): Uni<Void> {
        return requestStore.updateContact(pathParams.asSessionInfo(), contact)
    }

    @Auditable(action = NotarizationRequestAction.TASK_START)
    @POST
    @Path(TASK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @Operation(summary = "Start task", description = "Starts a task and returns information how to fullfill it.")
    @ResponseStatus(201)
    @APIResponse(
        responseCode = "201",
        description = "Information on how to fulfill the task.",
        content = [Content(schema = Schema(implementation = TaskInstance::class))]
    )
    @RequestBody(content = [Content(schema = Schema(implementation = VcTaskStart::class))])
    @Throws(
        AuthorizationException::class, InvalidRequestStateException::class
    )
    fun startTask(
        @Context request: UriInfo,
        @BeanParam @Valid pathParams: AccessPathParameters,
        @RestQuery @Valid taskId: TaskId,
        @Valid data: JsonNode?
    ): Uni<TaskInstance> {
        return workService.startTask(pathParams.asSessionInfo(), taskId, data)
    }

    @Auditable(action = NotarizationRequestAction.TASK_CANCEL)
    @DELETE
    @Path(TASK_PATH)
    @Operation(summary = "Cancel task", description = "Cancels a task.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The task was successfully cancelled.")
    @Throws(
        AuthorizationException::class, InvalidRequestStateException::class
    )
    fun cancelTask(
        @Context request: UriInfo,
        @BeanParam @Valid pathParams: AccessPathParameters,
        @RestQuery taskId: TaskId
    ): Uni<Void> {
        return workService.cancelTask(pathParams.asSessionInfo(), taskId)
    }

    @Auditable(action = NotarizationRequestAction.SUBMIT)
    @POST
    @Path(SUBMISSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @Operation(
        summary = "Submit a new notarization request",
        description = "Adds the submitted notarization request to the identified session."
    )
    @ResponseStatus(201)
    @APIResponse(
        responseCode = "201",
        description = "Information about the submission.",
        content = [Content(schema = Schema(implementation = SubmissionResponse::class))]
    )
    @APIResponse(ref = Api.Response.CANNOT_REUSE_TOKEN)
    @Throws(
        NotFoundException::class, InvalidJsonLdException::class, CannotReuseTokenException::class
    )
    fun submit(
        @Context request: UriInfo,
        @BeanParam @Valid pathParams: AccessPathParameters,
        @Valid submissionRequest: SubmitNotarizationRequest
    ): Uni<RestResponse<SubmissionResponse>> {
        registry.counter("request.submission.create-request.call").increment()
        val requestId = UUID.randomUUID()
        return requestStore.submitNewRequest(
            NotarizationRequestId(requestId),
            pathParams.asSessionInfo(),
            submissionRequest
        )
            .map { createdRequest: NotarizationRequest ->
                RestResponse.ResponseBuilder.created<SubmissionResponse>(
                    request.baseUriBuilder.path(Api.Path.SESSION_RESOURCE).path(REQUEST_PATH)
                        .build(pathParams.accessToken.token)
                )
                    .entity(from(createdRequest))
                    .build()
            }
    }

    @Auditable(action = NotarizationRequestAction.FETCH)
    @GET
    @Path(REQUEST_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Fetch notarization request",
        description = "Fetches the information of the identified notarization request."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "Information about the current request associated with the identified session.",
        content = [Content(schema = Schema(implementation = NotarizationRequestView::class))]
    )
    @Throws(
        NotFoundException::class, AuthorizationException::class
    )
    fun fetchNotarizationRequest(
        @BeanParam @Valid params: AccessPathParameters
    ): Uni<NotarizationRequestView> {
        registry.counter("request.submission.fetch-notarizationrequest-request.call").increment()
        return requestStore.fetchNotarizationRequest(params.asSessionInfo())
    }

    @Auditable(action = NotarizationRequestAction.UPDATE)
    @PUT
    @Path(REQUEST_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update notarization request",
        description = "Updates the notarization request, replacing previous information."
    )
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The notarization request was successfully updated.")
    @RequestBody(description = "The proposed content of the credential to be issued.")
    @Throws(
        InvalidRequestStateException::class,
        NotFoundException::class,
        InvalidJsonLdException::class,
        AuthorizationException::class
    )
    fun update(
        @BeanParam @Valid params: AccessPathParameters,
        @Valid data: JsonNode
    ): Uni<Void> {
        registry.counter("request.submission.update-request.call").increment()
        return requestStore.updateRequest(
            params.asSessionInfo(),
            data
        )
    }

    @Auditable(action = NotarizationRequestAction.ASSIGN_DID)
    @PUT
    @Path("$REQUEST_PATH/did-holder")
    @Consumes(
        MediaType.APPLICATION_JSON
    )
    @Operation(
        summary = "Assign DID holder",
        description = "Assigns the DID holder to the identified notarization request."
    )
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The holder information was successfully updated.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    fun assignDidHolder(
        @BeanParam @Valid params: AccessPathParameters,
        @Schema(description = "The target holder of the credential to be issued") @RestQuery didHolder: String,
        @Schema(description = "An invitation URL of the holder") @RestQuery invitation: String?
    ): Uni<Void> {
        registry.counter("request.submission.assign-did-holder.call").increment()
        return requestStore.assignDidHolder(params.asSessionInfo(), didHolder, invitation)
    }

    @Auditable(action = NotarizationRequestAction.MARK_READY)
    @POST
    @Path("$REQUEST_PATH/ready")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Mark ready",
        description = "Marks the identified notarization request as ready for notarization. If 'manualRelease' is true, an URL to release an accepted credential is returned."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "URL allowing to release the credential after acceptance by the notary if 'manualRelease' was set to true.",
        content = [Content(schema = Schema(implementation = MarkReadyResponse::class))]
    )
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    fun markReady(
        @BeanParam @Valid params: AccessPathParameters,
        @Schema(defaultValue = "false") @RestQuery manualRelease: Boolean?
    ): Uni<MarkReadyResponse> {
        registry.counter("request.submission.mark-ready-request.call").increment()
        return requestStore.markReady(params.asSessionInfo(), manualRelease ?: false)
    }

    @Auditable(action = NotarizationRequestAction.MARK_UNREADY)
    @POST
    @Path("$REQUEST_PATH/unready")
    @Operation(summary = "Mark unready", description = "Marks the identified notarization request as unready.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The status was successfully updated.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    fun markUnready(
        @BeanParam @Valid params: AccessPathParameters
    ): Uni<Void> {
        registry.counter("request.submission.mark-unready-request.call").increment()
        return requestStore.markUnready(params.asSessionInfo())
    }

    class AccessPathParameters {
        @RestHeader(Api.Header.ACCESS_TOKEN)
        @get:Schema(type = SchemaType.STRING)
        @NotNull
        lateinit var accessToken: AccessToken

        @RestPath(Api.Param.SESSION)
        @NotNull lateinit var session: SessionId

        fun asSessionInfo(): SessionInfo {
            return SessionInfo(session, accessToken)
        }
    }

    @Auditable(action = NotarizationRequestAction.MANUAL_RELEASE)
    @POST
    @Path(Api.Path.ISSUE_MANUALY + "/" + Api.Param.RELEASE_TOKEN_PARAM)
    @Operation(summary = "Trigger issuance", description = "Triggers the issuance of an accepted notarization request.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The manual release was successfully triggered.")
    @Throws(
        NotFoundException::class
    )
    fun manualRelease(
        @PathParam(Api.Param.RELEASE_TOKEN) releaseToken: String
    ): Uni<Void> {
        registry.counter("request.submission.release-issuance.call").increment()
        return requestStore.manualRelease(releaseToken)
    }

    @GET
    @Path("$REQUEST_PATH/ssiInviteUrl")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get ssi generated invite url(s)",
        description = "Gets ssi generated invite url - will be empty if invite was given at start of the process by requestor."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "Inviation URL for the issued credential.",
        content = [Content(schema = Schema(implementation = InvitationUrlResponse::class))]
    )
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class, AuthorizationException::class
    )
    fun getSsiInviteUrl(
        @BeanParam @Valid params: AccessPathParameters
    ): Uni<List<InvitationUrlResponse>> {
        return requestStore.getSsiInvitationUrl(params.asSessionInfo()).map { foundInvites ->

            foundInvites.filter { it.ssiInvitationUrl != null }
                .map { item -> InvitationUrlResponse(URI.create(item.ssiInvitationUrl!!), item.issuerVersion!!.name ) }
        }
    }

    companion object {
        const val SESSION_PATH = "/" + Api.Param.SESSION_PARAM
        const val SUBMISSION_PATH = SESSION_PATH + "/" + Api.Path.SUBMISSION
        const val IDENTIFY_PATH = SESSION_PATH + "/" + Api.Path.IDENTIFY
        const val REQUEST_PATH = SUBMISSION_PATH
        const val TASK_PATH = SESSION_PATH + "/" + Api.Path.TASK
    }
}
