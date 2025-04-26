/****************************************************************************
 * Copyright 2024 ecsec GmbH
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
 ***************************************************************************/

package eu.xfsc.not.compliance.rest.resources

import eu.xfsc.not.compliance.ApplicationConfig
import eu.xfsc.not.compliance.application.ComplianceManager
import eu.xfsc.not.compliance.rest.model.BeginResponse
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.json.JsonObject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriBuilder
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import java.net.URL


/**
 * @author Mike Prechtl
 */
@Path("")
class ComplianceTaskApi {

    companion object {
        const val NONCE_PARAM: String = "nonce"
        const val BEGIN_COMPLIANCE_TASK: String = "task/begin"
        const val SUBMIT_VERIFIABLE_PRESENTATION: String = "task/{$NONCE_PARAM}/vp/submit"
        const val CANCEL_COMPLIANCE_TASK: String = "task/{$NONCE_PARAM}/cancel"
    }

    @Inject
    lateinit var appConfig: ApplicationConfig

    @Inject
    lateinit var complianceManager: ComplianceManager

    @Operation(
        summary = "Begin a compliance task",
        description = "Begins a new compliance task"
    )
    @Path(BEGIN_COMPLIANCE_TASK)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun beginComplianceTask(
        @QueryParam("success") success: URL,
        @QueryParam("failure") failure: URL,
    ) : BeginResponse {
        val nonce = complianceManager.startComplianceTask(success, failure)
        return BeginResponse(
            uriBuilder()
                .path(SUBMIT_VERIFIABLE_PRESENTATION)
                .build(nonce),
            uriBuilder()
                .path(CANCEL_COMPLIANCE_TASK)
                .build(nonce)
        )
    }

    @Operation(
        summary = "Cancel a compliance task",
        description = "Cancels a compliance task"
    )
    @Path(CANCEL_COMPLIANCE_TASK)
    @DELETE
    @Blocking
    fun cancelComplianceTask(
        @PathParam(NONCE_PARAM) nonce: String,
    ) {
        complianceManager.cancelComplianceTask(nonce)
    }

    @Operation(
        summary = "Submit a verifiable presentation for compliance check",
        description = "Accepts a verifiable presentation which is submitted to the Gaia-X compliance service"
    )
    @Path(SUBMIT_VERIFIABLE_PRESENTATION)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    fun submitVerifiablePresentation(
        @PathParam(NONCE_PARAM) nonce: String,
        vp: JsonObject,
    ) {
        complianceManager.submitVerifiablePresentation(nonce, vp)
    }

    private fun uriBuilder() : UriBuilder {
        return appConfig.externalUrl()
            .map { UriBuilder.fromUri(it) }
            .orElseThrow { InternalServerErrorException("There is no external URL configured.") }
    }
}
