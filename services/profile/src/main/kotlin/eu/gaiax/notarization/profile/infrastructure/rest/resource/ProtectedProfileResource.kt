package eu.gaiax.notarization.profile.infrastructure.rest.resource

import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.profile.domain.exception.UnknownProfileException
import eu.gaiax.notarization.profile.domain.service.ProfileManagementService
import eu.gaiax.notarization.api.profile.ProfileApi
import eu.gaiax.notarization.profile.domain.model.assertTreesValid
import eu.gaiax.notarization.profile.infrastructure.rest.dto.ProfileDidRequest
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestPath

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@Path(ProfileApi.Path.PROTECTED_PROFILE_RESOURCE)
class ProtectedProfileResource(private val profileService: ProfileManagementService) {

    @PUT
    @Path(ProfileApi.Param.PROFILE_ID_PARAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Set profile",
        description = "Create or override a profile with the given identifier."
    )
    @ResponseStatus(204)
    @APIResponse(
        responseCode = "204",
        description = "The operation was successful.",
    )
    fun setProfile(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String, profile: Profile): Uni<Void> {
        logger.info { "Checking validity of profile $identifier" }
        profile.assertTreesValid()
        logger.info { "Updating content of profile $identifier" }
        return profileService.setProfile(identifier, profile)
    }

    @POST
    @Path(ProfileApi.Param.PROFILE_ID_PARAM + "/did")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Set DID for the profile",
        description = "A one-time operation per profile, initialising the DIDs to use."
    )
    @ResponseStatus(204)
    @APIResponse(
        responseCode = "204",
        description = "The operation was successful.",
    )
    fun setDid(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String, didRequest: ProfileDidRequest): Uni<Void> {

        return profileService.setDidInformation(identifier, didRequest)
    }
    @DELETE
    @Path(ProfileApi.Param.PROFILE_ID_PARAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete profile",
        description = "Delete a profile with the given identifier."
    )
    @ResponseStatus(204)
    @APIResponse(
        responseCode = "204",
        description = "A single profile.",
    )
    @Throws(
        UnknownProfileException::class
    )
    fun deleteProfile(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String): Uni<Void> {
        return profileService.deleteProfile(identifier)
    }
}
