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
 ***************************************************************************/

package eu.gaiax.notarization.api

import eu.gaiax.notarization.api.revocation.ListCredentialRequestData
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient


@RegisterRestClient(configKey = "ssi-issuance-api")
interface IssuanceController {

    @Path("list-credential/{profileName}/issue")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun issueListCredential(
        @PathParam("profileName") profileName: String,
        credentialRequest: ListCredentialRequestData
    ): String

}
