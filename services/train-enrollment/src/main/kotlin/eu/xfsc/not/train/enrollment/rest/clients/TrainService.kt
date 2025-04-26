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

package eu.xfsc.not.train.enrollment.rest.clients

import com.fasterxml.jackson.databind.JsonNode
import io.quarkus.oidc.client.filter.OidcClientFilter
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient


/**
 * @author Mike Prechtl
 */
@Path("")
@RegisterRestClient(configKey = "train")
@OidcClientFilter
interface TrainService {

    /**
     * See <a href="https://gitlab.eclipse.org/eclipse/xfsc/train/tspa/-/blob/master/src/main/java/eu/xfsc/train/tspa/controller/TrustListPublicationController.java?ref_type=heads#L213"/>
     * for more information about this endpoint.
     */
    @PUT
    @Path("tspa/v1/{framework-name}/trust-list/tsp")
    @Operation(
        summary = "Used to create a TSP for TRAIN enrollment."
    )
    @APIResponses(
        APIResponse(responseCode = "201", description = "Successfully published TSP."),
        APIResponse(responseCode = "400", description = "TSP validation failed."),
        APIResponse(responseCode = "404", description = "No Trust-List found for the provided framework name."),
        APIResponse(responseCode = "404", description = "Some internal error occurred."),
    )
    fun createTSP(
        @PathParam("framework-name") frameworkName: String,
        tspJson: JsonNode
    )

    /**
     * See <a href="https://gitlab.eclipse.org/eclipse/xfsc/train/tspa/-/blob/master/src/main/java/eu/xfsc/train/tspa/controller/TrustListPublicationController.java?ref_type=heads#L213"/>
     * for more information about this endpoint.
     */
    @PUT
    @Operation(
        summary = "Used to create a TSP for TRAIN enrollment."
    )
    @APIResponses(
        APIResponse(responseCode = "201", description = "Successfully published TSP."),
        APIResponse(responseCode = "400", description = "TSP validation failed."),
        APIResponse(responseCode = "404", description = "No Trust-List found for the provided framework name."),
        APIResponse(responseCode = "404", description = "Some internal error occurred."),
    )
    fun createTSPViaProvidedTrustListEndpoint(
        tspJson: JsonNode
    )
}
