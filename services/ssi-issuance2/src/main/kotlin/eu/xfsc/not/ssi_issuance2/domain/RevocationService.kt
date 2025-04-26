package eu.xfsc.not.ssi_issuance2.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("management")
@RegisterRestClient(configKey = "revocation_service")
interface RevocationService {
    @POST
    @Path("lists/{profile}/entry")
    @Produces("application/json")
    fun addStatusEntry(@PathParam("profile") profile: String): JsonNode
}
