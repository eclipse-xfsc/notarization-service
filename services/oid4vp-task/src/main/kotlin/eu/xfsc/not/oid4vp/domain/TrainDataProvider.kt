package eu.xfsc.not.oid4vp.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import eu.gaiax.notarization.api.profile.ProfileServiceHttpInterface
import eu.gaiax.notarization.api.profile.TrainParameter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient

private val logger = KotlinLogging.logger {}

@RegisterRestClient(configKey = "profile_api")
interface ProfileServiceClient : ProfileServiceHttpInterface

interface TrainDataProvider {
    fun getTrainParameter(profileId: String, taskName: String): TrainParameter?
}

@ApplicationScoped
class TrainDataProviderImpl : TrainDataProvider {
    @Inject
    lateinit var om: ObjectMapper
    @Inject
    @RestClient
    lateinit var profileService: ProfileServiceClient

    override fun getTrainParameter(profileId: String, taskName: String): TrainParameter? {
        val profile = profileService.fetchProfile(profileId).await().indefinitely()
        val taskDescription = profile.taskDescriptions.firstOrNull { it.name == taskName }
        return taskDescription?.extensions?.get("trainParameter")?.let {
            om.convertValue(it)
        }
    }

}
