package eu.xfsc.not.ssi_issuance2.domain

import eu.gaiax.notarization.api.issuance.ProfileIssuanceSpec
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileApi
import eu.gaiax.notarization.api.profile.ProfileApi.Path.SSI_DATA_V2
import eu.gaiax.notarization.api.profile.ProfileServiceHttpInterface
import io.quarkus.cache.CacheResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hibernate.UnknownProfileException
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestPath

private val log = KotlinLogging.logger {}

@RegisterRestClient(configKey = "profile_api")
@Path(ProfileApi.Path.V1_PREFIX)
interface ProfileServiceClient : ProfileServiceHttpInterface {
    @GET
    @Path(SSI_DATA_V2)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Throws(
        UnknownProfileException::class
    )
    fun fetchDids(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String): ProfileIssuanceSpec
}

interface ProfileProvider {
    fun fetchProfile(profileId: String): Profile?
    fun fetchDids(profileId: String): ProfileIssuanceSpec?
}
@ApplicationScoped
class ProfileProviderImpl : ProfileProvider {
    @Inject
    @RestClient
    lateinit var profileService: ProfileServiceClient

    @CacheResult(cacheName = "profile-cache")
    override fun fetchProfile(profileId: String): Profile? {
        try {
            return profileService.fetchProfile(profileId).await().indefinitely()
        } catch (e: Exception){
            log.warn(e) {"Failed to fetch profile: $profileId"}
            return null
        }
    }

    override fun fetchDids(profileId: String): ProfileIssuanceSpec? {
        return profileService.fetchDids(profileId)
    }
}
