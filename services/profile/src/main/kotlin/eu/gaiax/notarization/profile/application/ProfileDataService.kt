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
package eu.gaiax.notarization.profile.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.profile.domain.entity.ProfileDid
import eu.gaiax.notarization.profile.domain.service.ProfileManagementService
import eu.gaiax.notarization.profile.infrastructure.rest.client.RevocationHttpClient
import eu.gaiax.notarization.profile.infrastructure.rest.client.SsiIssuanceV1HttpClient
import eu.gaiax.notarization.profile.infrastructure.rest.client.SsiIssuanceV1HttpClient.DidInitRequest
import eu.gaiax.notarization.profile.infrastructure.rest.client.SsiIssuanceV2HttpClient
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}
/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
open class ProfileDataService {
    @Inject
    lateinit var profileService: ProfileManagementService

    @Inject
    lateinit var ssiIssuanceClient: Optional<SsiIssuanceV1HttpClient>
    @Inject
    lateinit var ssiIssuance2Client: Optional<SsiIssuanceV2HttpClient>

    @Inject
    lateinit var om: ObjectMapper

    @RestClient
    lateinit var revocationClient: RevocationHttpClient

    @WithTransaction
    fun requestUpdateOutstandingDids(): Uni<Long> {

        return findUnprocessedProfiles()
            .onItem().transformToUniAndConcatenate { profileName ->
                initDids(
                    profileName
                ).invoke { response ->
                    if (response == null) {
                        logger.warn { "Did not initialise DID of profile $profileName" }
                    }
                }
            }
            .onItem().transformToUniAndConcatenate { profileName ->
                initRevocation(
                    profileName
                ).invoke { response ->
                    if (response == null) {
                        logger.warn { "Did not initialise revocation list of profile $profileName" }
                    }
                }
            }
            .collect().with(Collectors.counting())
    }

    private fun findUnprocessedProfiles(): Multi<String> {
        return ProfileDid.find("issuanceContent is null or revocationListCreatedAt is null")
            .list()
            .map { foundItems ->
                val results = mutableSetOf<String>()
                for (item in foundItems) {
                    item.profileId?.let { results.add(it) }
                }
                logger.debug { "Found unprocessed profiles: $results" }
                results
            }
            .onItem().transformToMulti { foundItems ->

                Multi.createFrom().items(foundItems.stream())
            }
    }

    @WithTransaction
    fun initDids(profileId: String): Uni<String?> {
        return ProfileDid.findByProfileIdOptionally(profileId)
            .chain { profileDids ->
                Multi.createFrom().iterable(profileDids)
                    .onItem().transformToUniAndConcatenate { profileDid ->
                        if (profileDid?.issuanceContent == null) {

                            val jsonResp = if (profileDid.issuanceVersion == ApiVersion.V2) {
                                val client = ssiIssuance2Client.get()
                                val ssiIssuanceInit = SsiIssuanceV2HttpClient.requestFrom(
                                    profileId,
                                    profileDid.keyType,
                                    profileDid.signatureType)
                                client.initService(ssiIssuanceInit).map {
                                    om.valueToTree(it)
                                }
                            } else if (profileDid.issuanceVersion == ApiVersion.V1) {
                                val client =  ssiIssuanceClient.get()
                                client.initiate(DidInitRequest.from(profileId))
                            } else {
                                throw RuntimeException("Unsupported issuance version: ${profileDid.issuanceVersion}")
                            }

                            jsonResp.flatMap { response ->
                                if (response != null) {
                                    val currentRecord = if (profileDid == null) {
                                        val newRecord = ProfileDid()
                                        newRecord.id = UUID.randomUUID()
                                        newRecord.profileId = profileId
                                        newRecord
                                    } else {
                                        profileDid
                                    }
                                    currentRecord.issuanceContent = response
                                    return@flatMap currentRecord.persist<PanacheEntityBase>()
                                        .map { _ -> profileId }
                                } else {
                                    return@flatMap Uni.createFrom().nullItem<String>()
                                }
                            }
                        } else {
                            Uni.createFrom().item(profileId)
                        }
                    }.collect().asList().map { items ->
                        items.firstOrNull { it != null }
                    }
            }
            .onFailure().recoverWithUni { t: Throwable? ->
                logger.error(t) {"Could not initialize the DIDs of $profileId" }
                Uni.createFrom().nullItem()
            }
    }

    @WithTransaction
    fun initRevocation(profileId: String?): Uni<Response?> {
        return if (profileId == null) {
            Uni.createFrom().nullItem()
        } else ProfileDid.findByProfileId(profileId)
            .chain { profileDids ->
                Multi.createFrom().iterable(profileDids)
                    .onItem().transformToUniAndConcatenate { profileDid ->
                        revocationClient.initiate(profileId)
                            .onFailure(WebApplicationException::class.java)
                            .recoverWithItem { throwable: Throwable? ->
                                if (throwable is WebApplicationException) {
                                    val response = throwable.response
                                    if (response.status != Response.Status.CONFLICT.statusCode) {
                                        logger.error {
                                            "Received an unexpected error while initializing the revocation service for the profile" +
                                                " $profileId: [${response.status},${response.entity},${response.headers}]"
                                        }
                                        throw IllegalArgumentException(
                                            "Unexcepted response from revocation service",
                                            throwable
                                        )
                                    } else {
                                        return@recoverWithItem response
                                    }
                                } else {
                                    throw IllegalArgumentException(
                                        "Unexpected exception from revocation client",
                                        throwable
                                    )
                                }
                            }.chain { response ->
                                val status = response?.statusInfo
                                if (status != null && (status.family == Response.Status.Family.SUCCESSFUL || status.statusCode == 409)) {
                                    profileDid.revocationListCreatedAt = Instant.now()
                                    profileDid.persist<ProfileDid>()
                                        .map { response }
                                } else {
                                    Uni.createFrom().nullItem()
                                }
                            }
                    }.collect().asList().map { items ->
                        items.firstOrNull()
                    }
            }
            .onFailure().recoverWithUni { t: Throwable? ->
                logger.error(t) { "Could not initialize the revocation of $profileId" }
                Uni.createFrom().nullItem()
            }
    }

    class ProfileDidInit(
        val profileId: String,
        val response: JsonNode
    )
}
