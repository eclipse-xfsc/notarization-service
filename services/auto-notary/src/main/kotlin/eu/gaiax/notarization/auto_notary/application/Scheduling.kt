package eu.gaiax.notarization.auto_notary.application

import io.quarkus.scheduler.Scheduled
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class Scheduling {
    @Inject
    lateinit var autoApprovingService: AutoApprovingService

    @Scheduled(cron = "{cron.auto-accept}")
    fun approveOwnRequests(): Uni<Void> {
        return autoApprovingService.approveOwnRequests().replaceWithVoid()
    }

    @Scheduled(cron = "{cron.auto-approve-accept}")
    fun approveAvailableRequests(): Uni<Void> {
        return autoApprovingService.approveAvailableRequests().replaceWithVoid()
    }
}
