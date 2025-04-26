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
package eu.gaiax.notarization.request_processing.infrastructure.rest.mappers

import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.infrastructure.rest.mappers.problem_details.ProblemDetails
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@Provider
class NotFoundExceptionMapper : ExceptionMapper<NotFoundException> {
    @APIResponse(
        responseCode = "404",
        description = "Resource Not Found",
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = Schema(implementation = ProblemDetails::class)
        )]
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    override fun toResponse(x: NotFoundException): Response {
        logger.debug("Handled not found exception", x)
        return Response.status(Response.Status.NOT_FOUND).entity(
            ProblemDetails(
                "Resource Not Found",
                Response.Status.NOT_FOUND.statusCode,
                "Indicated " + (if (x.message!!.isBlank()) "resource" else x.message) + " could not be found",
                null
            )
        ).build()
    }
}
