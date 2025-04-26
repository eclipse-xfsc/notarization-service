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

import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.ListMapping
import io.quarkus.cache.CacheResult
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.temporal.ChronoUnit

/**
 *
 * @author Florian Otto
 */
@Path("/management")
@RegisterRestClient(configKey = "revocation-api")
interface RevocationServiceRestClient {
    @get:CacheResult(cacheName = "listcred-profile-mapping-response")
    @get:GET
    @get:ClientHeaderParam(name = "Accept", value = [MediaType.APPLICATION_JSON])
    @get:Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @get:Path("lists")
    val list: Uni<List<ListMapping>>

    @Path("lists/{profileName}/entry/{idx}")
    @DELETE
    fun revoke(@PathParam("profileName") profileName: String, @PathParam("idx") idx: Long): Uni<Void>
}
