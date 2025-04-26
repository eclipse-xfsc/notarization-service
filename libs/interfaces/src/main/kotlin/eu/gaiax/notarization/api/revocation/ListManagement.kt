package eu.gaiax.notarization.api.revocation

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import java.time.Instant


@Path("management")
interface ListManagement {

    @Operation(
        summary = "Listing of the managed lists",
        description = "Retrieves a list of pairs containing the profile identifier and the associated list name.",
    )

    @Path("lists")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getLists(): List<ListMapping>


    @Operation(
        summary = "Registers a new list",
        description = "Register a new list for the given profile. This call initiates the database for the new list. A ListCredential is not created.",
    )
    @APIResponse(
        responseCode = "200",
        description = "Returns the list name of the newly created list.",
    )
    @APIResponse(
        responseCode = "409",
        description = "Returned in case the profile already exists.",
    )
    @APIResponse(
        responseCode = "500",
        description = "Returned in case the profile could not be created.",
    )

    @Path("lists")
    @POST
    fun registerList(
        @QueryParam("profile") profileName: String,
        @QueryParam("issue-list-credential") @DefaultValue("true") issueCredential: Boolean,
    ): String


    @Operation(
        summary = "Start issuance of all ListCredentials",
        description = "Given the right conditions (e.i. renewal interval reached and changes in the revocation data present), a new ListCredential is requested from the issuance service. This call checks the conditions for all managed lists.",
    )
    @APIResponse(
        responseCode = "204",
        description = "Returned when the issuance process is finished.",
    )

    @Path("lists/issue-credentials")
    @POST
    fun issueCredentials()


    @Operation(
        summary = "Start issuance of one specific ListCredential",
        description = """
            Given the right conditions (e.i. renewal interval reached and changes in the revocation data present), a new
            ListCredential is requested from the issuance service. This call checks the conditions for one managed list.
        """,
    )
    @APIResponse(
        responseCode = "204",
        description = "Returned when the issuance process is finished.",
    )
    @APIResponse(
        responseCode = "404",
        description = "Returned if the profile does not exist.",
    )

    @Path("lists/issue-credential/{profileName}")
    @POST
    fun issueCredential(
        @PathParam("profileName") profileName: String,
        @QueryParam("force") @DefaultValue("false") force: Boolean
    )


    @Path("lists/{profileName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getListDefinition(
        @PathParam("profileName") profileName: String
    ): ListDefinition


    @Operation(
        summary = "Register a new credential with the list",
        description = "When this method is called, a new list entry is created and the CredentialStatus element described in the W3C specification is returned, so that it can be included in the new credential.",
    )
    @APIResponse(
        responseCode = "200",
        description = "Returns the CredentialStatus object of the added entry.",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = CredentialStatus::class),
                example = """
                    {
                        "type": "StatusList2021Entry",
                        "statusPurpose": "revocation",
                        "statusListIndex": "94567",
                        "statusListCredential": "https://revocation.example.com/status/somelistname"
                    }
                """,
            )
        ]
    )
    @APIResponse(
        responseCode = "404",
        description = "Returned if the list does not exists.",
    )

    @Path("lists/{profileName}/entry")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun addStatusEntry(
        @PathParam("profileName") profileName: String,
    ): CredentialStatus


    @Path("lists/{profileName}/encoded")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getEncodedList(
        @PathParam("profileName") profileName: String,
    ): String


    @Path("lists/{profileName}/entry/{idx}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getStatusEntry(
        @PathParam("profileName") profileName: String,
        @PathParam("idx") idx: Long,
    ): ListEntry


    @Operation(
        summary = "Revokes the referenced Credential",
        description = "The URL pointing to this list is the statusListCredential value in the StatusListEntry.",
    )
    @APIResponse(
        responseCode = "204",
        description = "Returned if the entry has been set to revoked.",
    )
    @APIResponse(
        responseCode = "404",
        description = "Returned if neither the list, nor the index in this list exists.",
    )

    @Path("lists/{profileName}/entry/{idx}")
    @DELETE
    fun revoke(
        @PathParam("profileName") profileName: String,
        @PathParam("idx") idx: Long,
    )

}

data class ListEntry(
    @JsonProperty("index")
    var index: Long,

    @JsonProperty("createdAt")
    var createdAt: Instant,

    @JsonProperty("revoked")
    var revoked: Boolean,

    @JsonProperty("revokedAt")
    var revokedAt: Instant,

    @JsonProperty("processed")
    var processed: Boolean,
)

data class ListDefinition (
    var listName: String,
    var profileName: String,
    var encodedList: String?,
    var listCredential: String?,
    var lastUpdate: Instant,
)


data class ListMapping(
    @JsonProperty("profile-name")
    var profileName: String,
    @JsonProperty("list-name")
    var listName: String,
)
