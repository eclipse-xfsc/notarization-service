package eu.gaiax.notarization.request_processing.infrastructure.rest.mappers

import eu.gaiax.notarization.request_processing.domain.exception.UnconfiguredProfileException
import eu.gaiax.notarization.request_processing.domain.exception.UnconfiguredServiceException
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

@Provider
class UnconfiguredServiceExceptionMapper: ExceptionMapper<UnconfiguredServiceException> {

    @APIResponse(
        responseCode = "400",
        description = "Service not yet configured or no longer available",
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = Schema(implementation = ProblemDetails::class)
        )]
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    override fun toResponse(x: UnconfiguredServiceException): Response {
        logger.error (x) { "Handled unconfigured service exception for profile ${x.profileId} called ${x.name}" }
        return Response.status(Response.Status.BAD_REQUEST).entity(
            ProblemDetails(
                "Unconfigured profile",
                Response.Status.BAD_REQUEST.statusCode,
                "Profile ${x.profileId} refers to service ${x.name}, but it is either not yet configured or no longer available",
                null
            )
        ).build()
    }
}
