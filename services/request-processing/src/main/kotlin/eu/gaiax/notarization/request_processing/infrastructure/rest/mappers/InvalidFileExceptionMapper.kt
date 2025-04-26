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

import eu.gaiax.notarization.request_processing.domain.exception.InvalidFileException
import eu.gaiax.notarization.request_processing.infrastructure.rest.mappers.problem_details.ProblemDetails
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
class InvalidFileExceptionMapper : ExceptionMapper<InvalidFileException> {
    @APIResponse(
        responseCode = "400",
        description = "Invalid file",
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = Schema(implementation = ProblemDetails::class)
        )]
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    override fun toResponse(x: InvalidFileException): Response {
        logger.debug("Handled invalid file exception", x)
        return Response.status(400)
            .entity(asDetails(x))
            .build()
    }

    @ServerExceptionMapper
    fun mapException(x: InvalidFileException): RestResponse<ProblemDetails> {
        logger.debug("Handled invalid file exception", x)
        return RestResponse.status(Response.Status.BAD_REQUEST, asDetails(x))
    }

    private fun asDetails(x: InvalidFileException): ProblemDetails {
        return ProblemDetails(
            ProblemDetails.Companion.ABOUT_BLANK,
            "Invalid file",
            400,
            "The provided profile identification does not identify a known profile.",
            null
        )
    }
}
