package eu.gaiax.notarization.profile.infrastructure.rest.resource

import eu.gaiax.notarization.profile.domain.exception.UnknownProfileException
import io.smallrye.mutiny.Uni
import jakarta.persistence.NoResultException
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

class ExceptionMappers {
    @ServerExceptionMapper
    fun mapException(x: NoResultException): RestResponse<String> {
        return RestResponse.status(Response.Status.NOT_FOUND, "Unknown profile")
    }
    @ServerExceptionMapper
    fun mapException(x: UnknownProfileException): RestResponse<String> {
        return RestResponse.status(Response.Status.NOT_FOUND, String.format("Unknown profile %s", x.profileId))
    }
}
