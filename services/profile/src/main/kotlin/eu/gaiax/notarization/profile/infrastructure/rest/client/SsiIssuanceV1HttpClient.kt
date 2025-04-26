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
package eu.gaiax.notarization.profile.infrastructure.rest.client

import com.fasterxml.jackson.databind.JsonNode
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

/**
 *
 * @author Neil Crossley
 */
@Path("")
@RegisterRestClient(configKey = "ssi-issuance-api-v1")
interface SsiIssuanceV1HttpClient {
    @Path("profile/init")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    fun initiate(request: DidInitRequest): Uni<JsonNode>
    class DidInitRequest (
        var profileID: String
    ) {

        companion object {
            fun from(profileId: String): DidInitRequest {
                val result = DidInitRequest(profileId)
                return result
            }
        }
    }
}
