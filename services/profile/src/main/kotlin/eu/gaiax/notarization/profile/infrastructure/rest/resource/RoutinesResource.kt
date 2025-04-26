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
package eu.gaiax.notarization.profile.infrastructure.rest.resource

import eu.gaiax.notarization.profile.application.ProfileDataService
import eu.gaiax.notarization.api.profile.ProfileApi
import eu.gaiax.notarization.profile.infrastructure.rest.openapi.OpenApiCorrection
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.*
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.reactive.ResponseStatus

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@Tag(name = OpenApiCorrection.CDI_TAG)
@Path(ProfileApi.Path.ROUTINE_RESOURCE)
@Schema(hidden = true)
class RoutinesResource {
    @Inject
    lateinit var profileData: ProfileDataService
    @POST
    @Path("request-init-profiles")
    @Operation(summary = "Outstanding DIDs", description = "Initalizes profiles without a did.")
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A count of changes.",
        content = [Content(schema = Schema(implementation = ResultsResponse::class))]
    )
    @Schema(hidden = true)
    fun requestOutStandingDids(): Uni<ResultsResponse> {
        logger.info("Request for outstanding dids was called.")
        return profileData.requestUpdateOutstandingDids()
            .map { ResultsResponse(it) }
    }

}
