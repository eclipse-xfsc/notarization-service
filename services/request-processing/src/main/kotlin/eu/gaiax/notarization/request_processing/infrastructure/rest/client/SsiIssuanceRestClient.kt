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
package eu.gaiax.notarization.request_processing.infrastructure.rest.client

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.net.URI
import java.time.Instant

/**
 *
 * @author Neil Crossley
 */
@Path("/")
interface SsiIssuanceRestClient {
    @POST
    @Path("credential/start-issuance/")
    @Consumes(MediaType.APPLICATION_JSON)
    fun sendIssuanceRequest(request: IssuanceRequest): Uni<IssuanceResponse>
    class IssuanceRequest {
        var profileID: ProfileId? = null
        var credentialData: JsonNode? = null
        var issuanceTimestamp: Instant? = null
        var holderDID: String? = null
        var invitationURL: String? = null
        var successURL: String? = null
        var failureURL: String? = null
    }

    class Document {
        var hash: String? = null
        var location: URI? = null
    }
    class IssuanceResponse {
        @get:Schema(required = false)
        var invitationURL: URI? = null
    }
}
