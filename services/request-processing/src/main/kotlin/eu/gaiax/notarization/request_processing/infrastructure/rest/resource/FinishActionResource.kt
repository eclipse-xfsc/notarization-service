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
import eu.gaiax.notarization.request_processing.application.taskprocessing.WorkExecutionEngine
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.*
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.reactive.*

/**
 *
 * @author Neil Crossley
 */
@Tag(name = Api.Tags.MANAGEMENT)
@Path(Api.Path.FINISH_ACTION_RESOURCE_WITH_NONCE)
class FinishActionResource {
    @Inject
    lateinit var workEngine: WorkExecutionEngine

    @Auditable(action = NotarizationRequestAction.ACTION_FINISH_SUCCESS)
    @POST
    @Path(Api.Path.SUCCESS)
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The action completion was successfully processed.")
    @RequestBody(description = "The action-specific payload upon success. For example, for a train enrolment action, this might be the enrolment data.")
    fun success(
        @PathParam(Api.Param.NONCE) nonce: String,
        data: JsonNode?
    ): Uni<Void> {
        return workEngine.finishWorkSuccess(nonce, data)
    }

    @Auditable(action = NotarizationRequestAction.ACTION_FINISH_FAIL)
    @POST
    @Path(Api.Path.FAIL)
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The task failure was successfully processed.")
    @RequestBody(description = "The action-specific payload upon failure, if any.")
    fun fail(
        @PathParam(Api.Param.NONCE) nonce: String,
        data: JsonNode?
    ): Uni<Void> {
        return workEngine.finishWorkFail(nonce, data)
    }
}
