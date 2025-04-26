package eu.gaiax.notarization.auto_notary.web.resources

import eu.gaiax.notarization.auto_notary.application.AutoApprovingService
import eu.gaiax.notarization.auto_notary.application.model.ApprovalResults
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/trigger")
class TriggerResource {
    @Inject
    lateinit var autoApprovingService: AutoApprovingService
    @POST
    @Path("/available")
    @Produces(MediaType.APPLICATION_JSON)
    fun triggerAvailable(): Uni<ApprovalResults> {
        return autoApprovingService.approveAvailableRequests()
    }

    @POST
    @Path("/ownClaimed")
    @Produces(MediaType.APPLICATION_JSON)
    fun triggerOwn(): Uni<ApprovalResults> {
        return autoApprovingService.approveOwnRequests()
    }
}
