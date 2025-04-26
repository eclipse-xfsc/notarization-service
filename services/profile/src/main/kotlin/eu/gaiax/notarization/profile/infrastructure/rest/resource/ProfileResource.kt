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
package eu.gaiax.notarization.profile.infrastructure.rest.resource

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.gaiax.notarization.profile.domain.entity.ProfileDid
import eu.gaiax.notarization.profile.domain.exception.UnknownProfileException
import eu.gaiax.notarization.profile.domain.service.ProfileManagementService
import eu.gaiax.notarization.api.profile.ProfileApi
import eu.gaiax.notarization.api.profile.NoFilter
import eu.gaiax.notarization.api.profile.ProfileServiceHttpInterface
import eu.gaiax.notarization.profile.infrastructure.rest.dto.SSIData
import io.smallrye.mutiny.Uni
import jakarta.annotation.Nullable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestPath

/**
 *
 * @author Neil Crossley
 */
@Path(ProfileApi.Path.V1_PREFIX)
class ProfileResource(private val profileService: ProfileManagementService): ProfileServiceHttpInterface {

    @GET
    @Path(ProfileApi.Path.PROFILES)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Fetch profiles", description = "Fetches available profiles using paging.")
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A page of the available profiles.",
    )
    override fun list(
        @Nullable @QueryParam("index") @Min(0) index: Int?,
        @Nullable @QueryParam("size") @Min(1) @Max(100) size: Int?,
        @Nullable @QueryParam("sort") sort: SortDirection?
    ): Uni<PagedView<Profile, NoFilter>> {
        return profileService.list(
            index?.coerceAtLeast(0) ?: 0,
            size?.coerceAtLeast(1) ?: 10,
            sort ?: SortDirection.Ascending
        )
    }

    @GET
    @Path(ProfileApi.Path.PROFILES + "/" + ProfileApi.Param.PROFILE_ID_PARAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Single profile",
        description = "Fetches a single profile."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A single profile.",
        content = [Content(
            schema = Schema(implementation = Profile::class)
        )]
    )
    @Throws(
        UnknownProfileException::class
    )
    override fun fetchProfile(@RestPath(ProfileApi.Param.PROFILE_ID) id: String): Uni<Profile> {
        return profileService.fetchProfile(id)
            .map { profile ->
                profile ?: throw UnknownProfileException(id)
            }
    }

    @GET
    @Path(ProfileApi.Path.SSI_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "DID summary of a profile",
        description = "Fetches the DID summary for the identified profile."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A DID summary of a profile.",
        content = [Content(schema = Schema(implementation = SSIData::class))]
    )
    @Throws(
        UnknownProfileException::class
    )
    fun fetchDids(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String): Uni<SSIData> {
        return ProfileDid.findByProfileId(identifier)
            .map { values ->
                var v1: JsonNode? = null
                var v2: JsonNode? = null
                for (value in values) {
                    when(value.issuanceVersion) {
                        ApiVersion.V1 -> v1 = value.issuanceContent
                        ApiVersion.V2 -> v2 = value.issuanceContent
                        else -> continue
                    }
                }
                SSIData(v1=v1, v2=v2)
            }
    }

    @GET
    @Path(ProfileApi.Path.SSI_DATA_V1)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Issuance V1 of the DID summary of the profile",
        description = "Fetches the issuance v1 DID summary for the identified profile."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A DID summary of the profile.",
        content = [Content(schema = Schema(type = SchemaType.OBJECT))]
    )
    @Throws(
        UnknownProfileException::class
    )
    fun fetchDidV1(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String): Uni<JsonNode> {
        return ProfileDid.findByProfileIdAndVersion(identifier, ApiVersion.V1)
            .map { values ->
                values?.issuanceContent ?: throw NotFoundException("Either the given profile does not exist, or the profile is not configured to issue V1 credentials")
            }
    }

    @GET
    @Path(ProfileApi.Path.SSI_DATA_V2)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Issuance V2 of the DID summary of the profile",
        description = "Fetches the issuance v2 DID summary for the identified profile."
    )
    @ResponseStatus(200)
    @APIResponse(
        responseCode = "200",
        description = "A DID summary of the profile.",
        content = [Content(schema = Schema(type = SchemaType.OBJECT))]
    )
    @Throws(
        UnknownProfileException::class
    )
    fun fetchDidV2(@RestPath(ProfileApi.Param.PROFILE_ID) identifier: String): Uni<JsonNode> {
        return ProfileDid.findByProfileIdAndVersion(identifier, ApiVersion.V2)
            .map { values ->
                values?.issuanceContent ?: throw NotFoundException("Either the given profile does not exist, or the profile is not configured to issue V2 credentials")
            }
    }
    @GET
    @Path(ProfileApi.Path.PROFILE_IDENTIFIERS)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "All profile identifiers",
        description = "The identifiers of all the profiles."
    )
    @ResponseStatus(200)
    override fun listProfileIdentifiers(): Uni<List<String>> {
        return this.profileService.listProfileIdentifiers()
    }

}
