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

package eu.xfsc.not.compliance.rest.client

import jakarta.json.JsonObject
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient


/**
 * @author Mike Prechtl
 */
@Path("api")
@RegisterRestClient(configKey = "compliance")
interface ComplianceService {

    @POST
    @Path("credential-offers")
    @Operation(
        summary = "Check Gaia-X compliance rules and outputs a VerifiableCredentials from your VerifiablePresentation"
    )
    @APIResponses(
        APIResponse(responseCode = "201", description = "Successfully signed VC."),
        APIResponse(responseCode = "400", description = "Invalid JSON request body."),
        APIResponse(responseCode = "409", description = "Invalid Participant Self Description."),
    )
    fun checkCompliance(
        @QueryParam("vcid") vcid: String?,
        verifiablePresentation: JsonObject
    ) : JsonObject

}
