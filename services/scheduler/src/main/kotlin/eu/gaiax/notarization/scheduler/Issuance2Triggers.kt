package eu.gaiax.notarization.scheduler

import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.Scheduled.ConcurrentExecution
import jakarta.enterprise.context.ApplicationScoped
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RestClient

private val log = KotlinLogging.logger {}

@ApplicationScoped
class Issuance2Triggers {
    @RestClient
    lateinit var issuance2 : Issuance2Routines

    @Scheduled(cron = "{cron.issuance2.timeoutSessionsTrigger}", concurrentExecution = ConcurrentExecution.SKIP)
    fun pruneTimeoutSessions(){
        log.info { "Sending prune timeout session trigger." }
        issuance2.pruneTimeoutSessions()
    }
}
