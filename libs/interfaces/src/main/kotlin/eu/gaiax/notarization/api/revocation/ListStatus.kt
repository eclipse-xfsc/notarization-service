package eu.gaiax.notarization.api.revocation

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse


@Path("status")
interface ListStatus {

    @Operation(
        summary = "Retrieves the current ListCredential",
        description = "The URL pointing to this list is the statusListCredential value in the StatusListEntry.",
    )
    @APIResponse(
        responseCode = "200",
        description = "Returns the currently issued ListCredential for the requested list.",
    )
    @APIResponse(
        responseCode = "404",
        description = "Returned if either the list does not exists, or no ListCredential has been issued yet.",
    )

    @Path("{listName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun getList(@PathParam("listName") listName: String): String

}
