package eu.gaiax.notarization.request_processing.infrastructure.rest.mappers

import eu.gaiax.notarization.request_processing.domain.exception.ForbiddenException
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
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
 * @author Mike Prechtl
 */
@Provider
class ForbiddenExceptionMapper : ExceptionMapper<ForbiddenException> {
    @APIResponse(
        responseCode = "403",
        description = "Forbidden",
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = Schema(implementation = ProblemDetails::class)
        )]
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    override fun toResponse(e: ForbiddenException): Response {
        logger.debug("Handled forbidden exception", e)
        return Response.status(Response.Status.FORBIDDEN).entity(
            ProblemDetails(
                "Forbidden",
                Response.Status.FORBIDDEN.statusCode,
                "Authorization not valid" + " - " + e.message,
                null
            )
        )
            .header(Api.Param.SESSION, e.id?.id ?: "no notarization request session")
            .build()
    }
}