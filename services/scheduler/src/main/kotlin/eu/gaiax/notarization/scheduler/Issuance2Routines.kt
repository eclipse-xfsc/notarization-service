package eu.gaiax.notarization.scheduler

import jakarta.ws.rs.DELETE
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "issuance2-api")
interface Issuance2Routines {
    @DELETE
    @Path("/api/v2/issuance/routines/pruneTimeoutSessions")
    fun pruneTimeoutSessions()
}
