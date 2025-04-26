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

import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.mappers.problem_details.InvalidRequestStateProblemDetails
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
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@Provider
class InvalidRequestStateMapper : ExceptionMapper<InvalidRequestStateException> {
    @APIResponse(
        responseCode = "400",
        description = "Invalid Notarization Request State",
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = Schema(implementation = InvalidRequestStateProblemDetails::class)
        )]
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    override fun toResponse(x: InvalidRequestStateException): Response {
        return Response.status(InvalidRequestStateProblemDetails.Companion.STATUS)
            .entity(asDetails(x))
            .header(Api.Param.SESSION, x.id.id)
            .build()
    }

    @ServerExceptionMapper
    fun mapException(x: InvalidRequestStateException): RestResponse<ProblemDetails> {
        return RestResponse.ResponseBuilder.create<ProblemDetails>(
            InvalidRequestStateProblemDetails.Companion.STATUS,
            asDetails(x)
        )
            .header(Api.Param.SESSION, x.id.id)
            .build()
    }

    companion object {
        private fun asDetails(x: InvalidRequestStateException): InvalidRequestStateProblemDetails {
            logger.debug("Handled invalid request state exception", x)
            return InvalidRequestStateProblemDetails(
                ProblemDetails.Companion.ABOUT_BLANK,
                "Invalid Notarization Request State",
                x.message,
                null,
                x.expected,
                x.actual!!
            )
        }
    }
}
