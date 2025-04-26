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

import io.smallrye.mutiny.Uni
import jakarta.json.JsonValue
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

/**
 *
 * @author Neil Crossley
 */
@Path("")
@RegisterRestClient(configKey = "dss-api")
interface DssRestClient {
    @POST
    @Path("/validation/validateSignature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun validateSignature(request: ValidateSignatureRequest): Uni<JsonValue>
    class ValidateSignatureRequest {
        var policy: Policy? = null
        var tokenExtractionStrategy = "NONE"
        var signatureId: String? = null
        var signedDocument: SignedDocument? = null
        var originalDocuments: List<OriginalDocument>? = null
    }

    class Policy {
        var bytes: String? = null
        var digestAlgorithm: String? = null
        var name: String? = null
    }

    class SignedDocument {
        var bytes: String? = null
        var digestAlgorithm: String? = null
        var name: String? = null
    }

    // The original file(s) in case of detached signature.
    class OriginalDocument {
        var bytes: ByteArray? = null
        var digestAlgorithm: String? = null
        var name: String? = null
    }
}
