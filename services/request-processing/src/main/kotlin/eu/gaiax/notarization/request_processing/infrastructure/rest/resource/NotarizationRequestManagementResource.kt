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
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.profile.NoFilter
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileApi
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.gaiax.notarization.request_processing.application.NotarizationManagementStore
import eu.gaiax.notarization.request_processing.application.WorkService
import eu.gaiax.notarization.request_processing.domain.exception.AuthorizationException
import eu.gaiax.notarization.request_processing.domain.exception.InvalidRequestStateException
import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskInstance
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import io.micrometer.core.instrument.MeterRegistry
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.eclipse.microprofile.openapi.annotations.tags.Tags
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery
import org.jetbrains.annotations.Nullable
import java.net.URISyntaxException

/**
 *
 * @author Neil Crossley
 */
@Tag(name = Api.Tags.MANAGEMENT)
@Path(Api.Path.V1_PREFIX)
class NotarizationRequestManagementResource(
    val managementStore: NotarizationManagementStore,
    val workService: WorkService,
    val registry: MeterRegistry,
    val profiles: ProfileService,
    val logger: Logger
) {
    @Tags(Tag(name = Api.Tags.SUBMISSION), Tag(name = Api.Tags.MANAGEMENT))
    @Path("/profiles")
    @PermitAll
    @GET
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @Operation(summary = "List profiles", description = "Fetches a paged result of the available profiles.")
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A list of all profiles.",
        content = [Content(schema = Schema(implementation = Profile::class))]
    )
    fun list(
        @Nullable @QueryParam("index") @Min(0) index: Int?,
        @Nullable @QueryParam("size") @Min(1) @Max(100) size: Int?,
        @Nullable @QueryParam("sort") sort: SortDirection?
    ): Uni<PagedView<Profile, NoFilter>> {
        return profiles.list(size = size, index = index, sort = sort)
    }

    @Auditable(action = NotarizationRequestAction.NOTARY_FETCH_ALL)
    @GET
    @Path(REQUESTS)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Query notarization requests",
        description = "Querys the available notarization requests. This operation will page the resuls and supports filtering."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A result list of notarization requests.",
        content = [Content(schema = Schema(implementation = PagedNotarizationRequestSummary::class))]
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun fetchAvailableRequests(
        @Schema(defaultValue = "0") @QueryParam("offset") @Min(value = 0) offset: Int?,
        @QueryParam("limit") @Min(value = 1) limit: Int?,
        @QueryParam("filter") filter: RequestFilter?,
        @Context securityContext: SecurityContext
    ): Uni<PagedNotarizationRequestSummary> {
        registry.counter("request.management.fetch-available-requests.call").increment()
        val effectiveFilter = filter ?: RequestFilter.available
        return managementStore.fetchAvailableRequests(offset ?: 0, limit ?: 25, effectiveFilter, securityContext)
    }

    @Auditable(action = NotarizationRequestAction.NOTARY_FETCH_REQ)
    @GET
    @Path(REQUEST)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Fetch available notarization request",
        description = "Fetches the available notarization request (not claimed by any notary operator)."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A notarization request.",
        content = [Content(schema = Schema(implementation = NotarizationRequestView::class))]
    )
    @Throws(
        NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun fetchAvailableRequest(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId
    ): Uni<NotarizationRequestView> {
        registry.counter("request.management.fetch-request.call").increment()
        return managementStore.fetchAvailableRequest(profile, id)
    }

    @Auditable(action = NotarizationRequestAction.NOTARY_FETCH_DOC)
    @GET
    @Path(DOC_REQUEST)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Fetch document", description = "Fetches an available document.")
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A document.",
        content = [Content(schema = Schema(implementation = DocumentFull::class))]
    )
    @Throws(
        NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun fetchAvailableDocument(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull notReqId: NotarizationRequestId,
        @RestPath(Api.Param.DOCUMENTID) @NotNull docId: DocumentId
    ): Uni<DocumentFull> {
        registry.counter("request.management.fetch-document.call").increment()
        return managementStore.fetchAvailableDocument(profile, notReqId, docId)
    }

    @Auditable(action = NotarizationRequestAction.CLAIM)
    @POST
    @Path("$REQUEST/claim")
    @Operation(
        summary = "Claim available notarization request",
        description = "Claims a notarization request current available (not claimed) by any notary operator."
    )
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The request was successfully claimed.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun claimAvailableRequest(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId,
        @Context securityContext: SecurityContext
    ): Uni<Void> {
        registry.counter("request.management.claim-request.call").increment()
        return managementStore.claimAvailableRequest(profile, id, securityContext)
    }

    @Auditable(action = NotarizationRequestAction.FETCH_IDENTITY)
    @GET
    @Path("$REQUEST/identity")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Fetch the identity assigned to the notarization request",
        description = "Fetches the encrypted identity assigned to the notarization request."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "An identity.",
        content = [Content(schema = Schema(implementation = IdentityView::class))]
    )
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun identity(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId
    ): Uni<Set<IdentityView>> {
        registry.counter("request.management.get-identity.call").increment()
        return managementStore.getIdentity(profile, id)
    }

    @Auditable(action = NotarizationRequestAction.ACCEPT)
    @POST
    @Path("$REQUEST/accept")
    @Consumes(MediaType.APPLICATION_JSON, MediaType.WILDCARD)
    @Operation(
        summary = "Accept the notarization request",
        description = "Accepts and approves the notarization request."
    )
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The notarization request was successfully accepted.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun acceptWithExtension(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId
    ): Uni<Void> {
        registry.counter("request.management.accept.call").increment()
        return managementStore.acceptAvailableRequest(profile, id)
    }

    @Auditable(action = NotarizationRequestAction.REJECT)
    @POST
    @Path("$REQUEST/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Reject the notarization request",
        description = "Rejects the notarization request in its current state. This does not delete the request. The business owner may alter and resubmit in cases where only details lead to rejection."
    )
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The notarization request was successfully rejected.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun reject(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId,
        request: RejectRequest?
    ): Uni<Void> {
        registry.counter("request.management.reject.call").increment()
        return managementStore.rejectAvailableRequest(profile, id, request?.reason)
    }

    @Auditable(action = NotarizationRequestAction.NOTARY_DELETE)
    @DELETE
    @Path(REQUEST)
    @Operation(
        summary = "Delete the notarization request",
        description = "Deletes the notarization request in its current state."
    )
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The notarization request was successfully deleted.")
    @Throws(
        InvalidRequestStateException::class, NotFoundException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun delete(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId
    ): Uni<Void> {
        registry.counter("request.management.delete.call").increment()
        return managementStore.deleteRequest(profile, id)
    }

    @Auditable(action = NotarizationRequestAction.NOTARY_REVOKE)
    @POST
    @Path(REVOKE)
    @Operation(summary = "Revoke the given credential", description = "Revokes the given credential.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The credential was successfully revoked.")
    @RequestBody(description = "The credential to be revoked. Must be issued with the fields 'statusListCredential' and 'statusListIndex'")
    @Throws(
        URISyntaxException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun revoke(
        @Context securityContext: SecurityContext,
        credential: JsonNode
    ): Uni<Void> {
        registry.counter("request.management.revoke-credential.call").increment()
        return managementStore.revokeRequest(securityContext, credential)
    }

    @Auditable(action = NotarizationRequestAction.ACTION_START)
    @POST
    @Path(ACTION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @Operation(summary = "Start action", description = "Starts a action and returns information how to fulfill it.")
    @ResponseStatus(201)
    @APIResponse(
        responseCode = "201",
        description = "Information on how to fulfill the action.",
        content = [Content(schema = Schema(implementation = TaskInstance::class))]
    )
    @Throws(
        AuthorizationException::class, InvalidRequestStateException::class
    )
    fun startAction(
        @Context request: UriInfo,
        @RestPath(Api.Param.PROFILE_ID) @NotNull profileId: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId,
        @RestQuery @Valid taskId: TaskId,
        @Valid data: JsonNode?
    ): Uni<TaskInstance> {
        return workService.startAction(profileId, id, taskId, data, request)
    }

    @Auditable(action = NotarizationRequestAction.ACTION_CANCEL)
    @DELETE
    @Path(ACTION_PATH)
    @Operation(summary = "Cancel action", description = "Cancels an action.")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The action was successfully cancelled.")
    @Throws(
        AuthorizationException::class, InvalidRequestStateException::class
    )
    fun cancelAction(
        @Context request: UriInfo,
        @RestPath(Api.Param.PROFILE_ID) @NotNull profileId: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId,
        @RestQuery taskId: TaskId,
        data: JsonNode
    ): Uni<Void> {
        return workService.cancelAction(profileId, id, taskId, data, request)
    }

    @Auditable(action = NotarizationRequestAction.ACTION_LIST)
    @GET
    @Path(ACTION_PATH)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @Operation(summary = "List actions", description = "Lists the current actions.")
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "List of the current actions.",
        content = [Content(schema = Schema(implementation = TaskInstance::class))]
    )
    @Throws(
        AuthorizationException::class, InvalidRequestStateException::class
    )
    fun listActions(
        @Context request: UriInfo,
        @RestPath(Api.Param.PROFILE_ID) @NotNull profileId: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId
    ): Uni<List<TaskInstance>> {
        return workService.listActions(profileId, id, request)
    }

    @Auditable(action = NotarizationRequestAction.CREDENTIAL_AUGMENTATION_PUT)
    @PUT
    @Path(CREDENTIAL_AUGMENTATION_PATH)
    @Operation(
        summary = "Assign a credential augmentation to the request",
        description = "Replace or set the structure that shall augment the requested credentials."
    )
    @ResponseStatus(204)
    @Consumes(
        MediaType.APPLICATION_JSON
    )
    @APIResponse(responseCode = "204", description = "The payload was accepted.")
    @RequestBody(
        description = "The payload to augment the requsted credential."
                + " The result of the augmentation must still conform to the schema of the credential."
                + " The augmentation and validation occur as a part of the issuing process.",
        content = [Content(schema = Schema(type = SchemaType.OBJECT))]
    )
    @Throws(
        URISyntaxException::class
    )
    @RolesAllowed("**")
    @SecurityRequirement(name = "notary", scopes = [Api.Role.NOTARY])
    fun assignCredentialAugmentation(
        @RestPath(Api.Param.PROFILE_ID) @NotNull profile: ProfileId,
        @RestPath(Api.Param.NOTARIZATION_REQUEST_ID) @NotNull id: NotarizationRequestId,
        credentialOverride: ObjectNode
    ): Uni<Void> {
        registry.counter("request.management.credential-override.put.call").increment()
        return managementStore.assignCredentialAugmentation(profile, id, credentialOverride)
    }

    companion object {
        const val REQUESTS = "/requests"
        const val REQUEST = ("/profiles/"
                + Api.Param.PROFILE_ID_PARAM
                + "/requests/"
                + Api.Param.NOTARIZATION_REQUEST_ID_PARAM)
        const val DOC_REQUEST = (REQUEST
                + "/document/"
                + Api.Param.NOTARIZATION_REQUEST_DOCUMENT_ID_PARAM)
        const val CREDENTIAL_AUGMENTATION_PATH = (REQUEST
                + Api.Path.CREDENTIAL_AUGMENTATION)
        const val REVOKE = "/revoke"
        const val ACTION_PATH = REQUEST + "/" + Api.Path.ACTIONS
    }
}
