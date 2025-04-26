package eu.gaiax.notarization.api.profile

import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import io.smallrye.mutiny.Uni
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jetbrains.annotations.Nullable

@Path(ProfileApi.Path.V1_PREFIX)
interface ProfileServiceHttpInterface {

    @GET
    @Path(ProfileApi.Path.PROFILES)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun list(
        @Nullable @QueryParam("index") @Min(0) index: Int?,
        @Nullable @QueryParam("size") @Min(1) @Max(100) size: Int?,
        @Nullable @QueryParam("sort") sort: SortDirection?
    ): Uni<PagedView<Profile, NoFilter>>

    @APIResponse(
        responseCode = "404",
        description = "The given profile id did not identify a known profile.",
    )
    @GET
    @Path("${ProfileApi.Path.PROFILES}/${ProfileApi.Param.PROFILE_ID_PARAM}")
    @Produces(MediaType.APPLICATION_JSON)
    fun fetchProfile(@PathParam(ProfileApi.Param.PROFILE_ID) id: String): Uni<Profile>

    @GET
    @Path(ProfileApi.Path.PROFILE_IDENTIFIERS)
    @Produces(MediaType.APPLICATION_JSON)
    fun listProfileIdentifiers(): Uni<List<String>>
}

@Path(ProfileApi.Path.V1_PREFIX)
interface ProfileServiceBlockingHttpInterface {

    @GET
    @Path(ProfileApi.Path.PROFILES)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun list(
        @Nullable @QueryParam("index") @Min(0) index: Int?,
        @Nullable @QueryParam("size") @Min(1) @Max(100) size: Int?,
        @Nullable @QueryParam("sort") sort: SortDirection?
    ): PagedView<Profile, NoFilter>

    @APIResponse(
        responseCode = "404",
        description = "The given profile id did not identify a known profile.",
    )
    @GET
    @Path("${ProfileApi.Path.PROFILES}/${ProfileApi.Param.PROFILE_ID_PARAM}")
    @Produces(MediaType.APPLICATION_JSON)
    fun fetchProfile(@PathParam(ProfileApi.Param.PROFILE_ID) id: String): Profile

    @GET
    @Path(ProfileApi.Path.PROFILE_IDENTIFIERS)
    @Produces(MediaType.APPLICATION_JSON)
    fun listProfileIdentifiers(): List<String>
}
