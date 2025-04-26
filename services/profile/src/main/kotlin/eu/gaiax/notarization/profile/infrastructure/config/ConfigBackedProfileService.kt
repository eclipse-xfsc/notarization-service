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
package eu.gaiax.notarization.profile.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import ellog.uuid.StandardUUID
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.profile.*
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.gaiax.notarization.profile.domain.entity.PersistantProfile
import eu.gaiax.notarization.profile.domain.entity.ProfileDid
import eu.gaiax.notarization.profile.domain.entity.objectMapper
import eu.gaiax.notarization.profile.domain.exception.UnknownProfileException
import eu.gaiax.notarization.profile.domain.model.assertTreesValid
import eu.gaiax.notarization.profile.domain.service.ProfileManagementService
import eu.gaiax.notarization.profile.infrastructure.config.ProfileSourceConfig.NotaryAccessConfig
import eu.gaiax.notarization.profile.infrastructure.config.ProfileSourceConfig.ProfileConfig
import eu.gaiax.notarization.profile.infrastructure.rest.dto.AutomaticDidCreationRequest
import eu.gaiax.notarization.profile.infrastructure.rest.dto.ProfileDidRequest
import eu.gaiax.notarization.profile.infrastructure.rest.dto.ProvidedDidRequest
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.panache.common.Sort
import io.quarkus.runtime.StartupEvent
import io.quarkus.vertx.VertxContextSupport
import io.smallrye.config.Priorities
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import mu.KotlinLogging
import org.jose4j.jwk.EllipticCurveJsonWebKey
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.jwk.RsaJsonWebKey
import java.util.*
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class ConfigBackedProfileService : ProfileManagementService {

    @Inject
    lateinit var profilesConfig: ProfileSourceConfig

    @Inject
    lateinit var mapper: ObjectMapper

    @Inject
    lateinit var issuanceVersions: Set<ApiVersion>

    @WithTransaction
    override fun list(index: Int, size: Int, sort: SortDirection): Uni<PagedView<Profile, NoFilter>> {
        val effectiveSort = if (sort == SortDirection.Ascending) {
            Sort.ascending()
        } else {
            Sort.descending()
        }

        val pageQuery = PersistantProfile.findAll(effectiveSort).page(index, size)

        return pageQuery.list()
            .chain { foundItems ->
                pageQuery.pageCount()
                    .chain { pageCount ->
                        pageQuery.count()
                            .map { total ->
                                val page = pageQuery.page()
                                val results = PagedView<Profile, NoFilter>()
                                results.index = page.index
                                results.size = page.size
                                results.total = total
                                results.pageCount = pageCount
                                results.sort = sort
                                results.items = foundItems.map {
                                    asProfile(it)
                                }
                                results
                        }
                }
        }
    }

    private fun asProfile(it: PersistantProfile) = Profile(
        it.profileId!!,
        it.capability,
        it.kind ?: it.capability?.asCredentialKind(),
        it.name!!,
        it.description!!,
        it.notaryRoles ?: setOf(),
        it.encryption!!,
        it.notaries!!,
        it.validFor!!,
        it.isRevocable!!,
        it.template!!,
        it.documentTemplate ?: "",
        it.taskDescriptions!!,
        tasks = it.tasks!!,
        preconditionTasks = it.preconditionTasks!!,
        preIssuanceActions = it.preIssuanceActions!!,
        postIssuanceActions = it.postIssuanceActions!!,
        actionDescriptions = it.actionDescriptions!!,
        extensions = it.extensions ?: emptyMap(),
    )

    override fun fetchProfile(id: String): Uni<Profile?> {
        return PersistantProfile.findByProfileIdOptionally(id)
            .map { profile ->
                if (profile != null) {
                    asProfile(profile)
                } else {
                    null
                }
            }
    }

    fun onStartup(@Observes @Priority(Priorities.APPLICATION) ev: StartupEvent) {

        logger.info { "Initialising ${profilesConfig.config().size} profiles on startup " }
        val allProfiles: MutableList<Profile> = ArrayList(
            profilesConfig.config().size
        )
        val candidateKeySpecs: MutableMap<String, MutableList<ProfileSourceConfig.KeyConfiguration>> = mutableMapOf()
        val requestAutomaticKeys = mutableSetOf<String>()
        for (profileConfig in profilesConfig.config()) {
            val profile = try {
                asProfile(profileConfig)
            } catch (ex: InvalidTaskTreeConfigurationException) {
                logger.error(ex) { "Could not convert the profile configuration ${profileConfig.id()}" }
                continue
            }
            allProfiles.add(profile)
        }
        for (keyConfig in profilesConfig.issuance()) {
            val item = candidateKeySpecs.computeIfAbsent(keyConfig.profileId()) { mutableListOf() }
            item.add(keyConfig)
        }
        for (profile in allProfiles) {
            val item: PersistantProfile = VertxContextSupport.subscribeAndAwait { ->
                Panache.withTransaction { ->
                    PersistantProfile.findByProfileIdOptionally(profile.id)
                        .chain { foundProfile ->
                            if (foundProfile == null) {
                                logger.info { "Adding new profile with id ${profile.id}" }
                                val newProfile = persistantProfile(null, profile.id, profile)
                                newProfile.persist<PersistantProfile>()
                            } else {
                                logger.info { "Skipping profile with id ${profile.id}" }
                                Uni.createFrom().nullItem()
                            }
                        }
                }
            } ?: continue
            val profileId = item.profileId!!
            val foundKeySpecs = candidateKeySpecs[profileId]
            logger.info { "Evaluating key spec $foundKeySpecs for profile $profileId" }
            if (foundKeySpecs != null) {
                val specifiedDids = mutableMapOf<ApiVersion, ObjectNode>()
                val specifiedVersions = mutableSetOf<ApiVersion>()
                for (keySpec in foundKeySpecs) {
                    val apiVersion: ApiVersion? = keySpec.version().getOrNull()
                    if (apiVersion != null) {
                        specifiedVersions.add(apiVersion)
                    }
                    if (keySpec.spec().isPresent) {
                        val spec = keySpec.spec().get()
                        if (apiVersion != null) {
                            specifiedDids[apiVersion] = spec
                        } else if (this.issuanceVersions.size == 1) {
                            specifiedDids[this.issuanceVersions.first()] = spec
                        } else {
                            logger.error { "A DID was specified for the profile ${item.profileId}, but the API version is missing. " +
                                "The API version is required because multiple issuance versions are supported." }
                        }
                    }
                }
                if (specifiedDids.isNotEmpty()) {
                    logger.info { "Persisting given key specs for profile $profileId" }
                    for (keySpec in specifiedDids) {
                        VertxContextSupport.subscribeAndAwait { ->
                            Panache.withTransaction { ->
                                val spec = keySpec.value
                                val profileDid = ProfileDid()
                                profileDid.id = UUID.randomUUID()
                                profileDid.issuanceVersion = keySpec.key
                                profileDid.profileId = profileId
                                profileDid.issuanceContent = keySpec.value
                                profileDid.persist()
                            }
                        }
                    }
                } else {
                    val automaticKeys = specifiedVersions.ifEmpty { this.issuanceVersions }
                    logger.info { "Automatically creating keys $automaticKeys for profile $profileId" }
                    for (apiVersion in automaticKeys) {
                        VertxContextSupport.subscribeAndAwait { ->
                            Panache.withTransaction { ->
                                val profileDid = ProfileDid()
                                profileDid.id = UUID.randomUUID()
                                profileDid.issuanceVersion = apiVersion
                                profileDid.setDefaults(apiVersion, profile.kind!!)
                                profileDid.profileId = profileId
                                profileDid.persist()
                            }.onFailure(IllegalArgumentException::class.java).recoverWithItem { t ->
                                logger.warn(t) { "Invalid combination of profile kind ${profile.kind} and api version $apiVersion for profile $profileId" }
                                null
                            }
                        }
                    }
                }
            }
        }
    }


    @Throws(InvalidTaskTreeConfigurationException::class)
    fun asProfile(profile: ProfileConfig): Profile {
        val result = Profile(
            profile.id(),
            profile.aip().getOrNull(),
            profile.kind().getOrElse { profile.aip().getOrNull()?.asCredentialKind() },
            profile.name(),
            profile.description(),
            profile.notaryRoles().getOrElse { setOf(profile.id()) },
            profile.encryption(),
            asNotaryAccess(profile.notaries()),
            profile.validFor().orElse(null),
            profile.isRevocable,
            profile.template(),
            profile.documentTemplate().orElse(null),
            profile.taskDescriptions().map {
                mapper.convertValue(it, TaskDescription::class.java)
            },
            profile.tasks(),
            profile.preconditionTasks(),
            profile.preIssuanceActions().orElse(ProfileTaskTree()),
            profile.postIssuanceActions().orElse(listOf()),
            profile.actionDescriptions().map {
                mapper.convertValue(it, IssuanceAction::class.java)
            }
        )
        result.assertTreesValid()
        return result
    }

    fun asNotaryAccess(access: List<NotaryAccessConfig>): List<NotaryAccess> {
        return access.map { item: NotaryAccessConfig -> this.asNotaryAccess(item) }
    }

    fun asNotaryAccess(access: NotaryAccessConfig): NotaryAccess {
        val jwk = access.jwk()
        val inputAlgorithm = access.algorithm().getOrNull()
        return asNotaryAccess(inputAlgorithm, jwk)
    }

    private fun asNotaryAccess(
        inputAlgorithm: String?,
        jwk: PublicJsonWebKey
    ): NotaryAccess {
        val algorithm: String = inputAlgorithm
            ?: profilesConfig.defaultAlgorithms().getOrDefault(
                jwk.keyType, FALLBACK_NOTARY_ALGORITHMS[jwk.keyType]
            )!!
        return NotaryAccess(algorithm, jwk)
    }

    @WithTransaction
    override fun deleteProfile(identifier: String): Uni<Void> {
        return PersistantProfile.delete("profileId", identifier).invoke { count ->
            if (count != 1L) {
                logger.info { "Tried to delete a profile, instead effected $count rows" }
                throw UnknownProfileException(identifier)
            }
        }.replaceWithVoid()
    }

    @WithTransaction
    override fun setProfile(identifier: String, profile: Profile): Uni<Void> {
        return PersistantProfile.findByProfileIdOptionally(identifier)
            .chain { persistedProfile: PersistantProfile? ->

                val currentProfile = persistantProfile(persistedProfile, identifier, profile)

                currentProfile.persist<PersistantProfile>()
                    .replaceWithVoid()
            }
    }

    override fun listProfileIdentifiers(): Uni<List<String>> {
        return PersistantProfile.allIdentifiers()
            .map { foundItems -> foundItems.map { it.profileId } }
    }

    @WithTransaction
    override fun setDidInformation(identifier: String, didRequest: ProfileDidRequest): Uni<Void> {
        return ProfileDid.findByProfileId(identifier)
            .chain { profileDids ->
                val didsByIssuanceVersion = profileDids.associateBy { it.issuanceVersion!! }

                PersistantProfile.findByProfileId(identifier)
                    .chain { profile ->
                        when(didRequest) {
                            is AutomaticDidCreationRequest -> {
                                val baselineVersions = (didRequest.versions ?: mutableSetOf()).ifEmpty {
                                    this.issuanceVersions
                                }
                                val effectiveVersions = baselineVersions - didsByIssuanceVersion.keys

                                if (effectiveVersions.isEmpty()) {
                                    throw BadRequestException("There are no issuance services configured")
                                }

                                Multi.createFrom().iterable(effectiveVersions)
                                    .onItem().transformToUniAndConcatenate { apiVersion ->
                                        val profileDid = ProfileDid()
                                        profileDid.id = UUID.randomUUID()
                                        profileDid.profileId = identifier
                                        profileDid.issuanceVersion = apiVersion
                                        profileDid.setDefaults(apiVersion, profile.kind!!)
                                        if (didRequest.keyType != null) {
                                            profileDid.keyType = didRequest.keyType
                                        }
                                        if (didRequest.signatureType != null) {
                                            profileDid.signatureType = didRequest.signatureType
                                        }
                                        profileDid.persistAndFlush<ProfileDid>()
                                    }.collect().asList().invoke { total ->
                                        logger.info { "Prepared profile $identifier for automatic DID creation for versions: $effectiveVersions" }
                                    }.replaceWithVoid()
                            }
                            is ProvidedDidRequest -> {
                                if (didRequest.v1 == null && didRequest.v2 == null) {
                                    throw BadRequestException("There are no issuance services configured")
                                }

                                val operation = if (didRequest.v1 != null) {
                                    var foundIssuance = didsByIssuanceVersion[ApiVersion.V1]
                                    if (foundIssuance == null) {
                                        foundIssuance = ProfileDid()
                                        foundIssuance.id = UUID.randomUUID()
                                        foundIssuance.profileId = identifier
                                        foundIssuance.issuanceVersion = ApiVersion.V1
                                    }
                                    foundIssuance.issuanceContent = didRequest.v1
                                    foundIssuance.persistAndFlush<ProfileDid>()
                                } else {
                                    Uni.createFrom().nullItem()
                                }
                                operation.chain { _ ->
                                    if (didRequest.v2 != null) {
                                        val issuanceSpec = didRequest.v2
                                        var foundIssuanceV2 = didsByIssuanceVersion[ApiVersion.V2]
                                        if (foundIssuanceV2 == null) {
                                            foundIssuanceV2 = ProfileDid()
                                            foundIssuanceV2.id = UUID.randomUUID()
                                            foundIssuanceV2.profileId = identifier
                                            foundIssuanceV2.issuanceVersion = ApiVersion.V2
                                        }
                                        foundIssuanceV2.issuanceContent = objectMapper.valueToTree(issuanceSpec)
                                        foundIssuanceV2.persistAndFlush<ProfileDid>()
                                    } else {
                                        Uni.createFrom().nullItem()
                                    }
                                }.replaceWithVoid()
                            }
                            else -> {
                                throw BadRequestException()
                            }
                        }
                    }
        }
    }

    private fun persistantProfile(
        persistedProfile: PersistantProfile?,
        identifier: String,
        profile: Profile
    ): PersistantProfile {
        val currentProfile = if (persistedProfile == null) {
            logger.info { "Creating new profile with identifier $identifier" }
            val newPersistedProfile = PersistantProfile()
            newPersistedProfile.id = StandardUUID.createTimeV7().toString()
            newPersistedProfile.profileId = identifier
            newPersistedProfile
        } else {
            logger.info { "Updating profile with identifier $identifier" }
            persistedProfile
        }

        currentProfile.name = profile.name
        currentProfile.description = profile.description
        currentProfile.kind = profile.kind ?: profile.aip?.asCredentialKind()
        currentProfile.capability = currentProfile.kind?.asProfile()
        currentProfile.encryption = profile.encryption
        currentProfile.validFor = profile.validFor
        currentProfile.isRevocable = profile.isRevocable
        currentProfile.notaries = profile.notaries.map { asNotaryAccess(it.algorithm, it.key) }
        currentProfile.notaryRoles = profile.notaryRoles
        currentProfile.template = profile.template
        currentProfile.documentTemplate = profile.documentTemplate
        currentProfile.preconditionTasks = profile.preconditionTasks
        currentProfile.tasks = profile.tasks
        currentProfile.taskDescriptions = profile.taskDescriptions
        currentProfile.preIssuanceActions = profile.preIssuanceActions
        currentProfile.postIssuanceActions = profile.postIssuanceActions
        currentProfile.actionDescriptions = profile.actionDescriptions
        currentProfile.extensions = profile.extensions
        return currentProfile
    }

    companion object {
        private val FALLBACK_NOTARY_ALGORITHMS = java.util.Map.of(
            RsaJsonWebKey.KEY_TYPE, "RSA-OAEP-256",
            EllipticCurveJsonWebKey.KEY_TYPE, "ECDH-ES+A256KW"
        )
    }
}
