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

import eu.gaiax.notarization.request_processing.application.NotarizationManagementStore
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.*
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.reactive.*

/**
 *
 * @author Florian Otto
 */
@Tag(name = Api.Tags.FINISH_NOTARIZATION_REQUEST)
@Path(Api.Path.FINISH_NOTARIZATION_REQUEST_RESOURCE_WITH_NONCE)
class FinishNotarizationRequestResource {
    @Inject
    lateinit var store: NotarizationManagementStore
    @Auditable(action = NotarizationRequestAction.ISSUANCE_FINISH_SUCCESS)
    @POST
    @Path(Api.Path.SUCCESS)
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The issuance status was successfully processed.")
    fun success(
        @PathParam(Api.Param.NONCE) nonce: String
    ): Uni<Void> {
        return store.finishRequestSuccess(nonce)
    }

    @Auditable(action = NotarizationRequestAction.ISSUANCE_FINISH_FAIL)
    @POST
    @Path(Api.Path.FAIL)
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "The issuance status was successfully processed.")
    fun fail(
        @PathParam(Api.Param.NONCE) nonce: String
    ): Uni<Void> {
        return store.finishRequestFail(nonce)
    }
}
