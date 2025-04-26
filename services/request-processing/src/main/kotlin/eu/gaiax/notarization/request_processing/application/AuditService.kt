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
package eu.gaiax.notarization.request_processing.application

import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.*
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.core.eventbus.EventBus
import jakarta.enterprise.context.ApplicationScoped
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.OffsetDateTime
import java.time.Period
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}
/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class AuditService(
    val registry: MeterRegistry,
    val bus: EventBus
) {
    @ConfigProperty(name = "notarization-processing.http.audit.logs.retention.period")
    lateinit var auditLogsRetentionPeriod: Period

    @ConsumeEvent(value = Channel.ON_AUDITABLE)
    @WithTransaction
    fun process(auditableEvent: OnAuditableHttp): Uni<Void> {
        logger.debug { "Persisting auditable event: [${auditableEvent.action};${auditableEvent.sessionId};${auditableEvent.notarizationRequestId}]" }
        val audit = HttpNotarizationRequestAudit()
        audit.id = auditableEvent.id
        audit.requestUri = auditableEvent.requestUri
        audit.sessionId = asStringId(auditableEvent.sessionId)
        audit.action = auditableEvent.action
        audit.notarizationId = asStringId(auditableEvent.notarizationRequestId)
        audit.ipAddress = auditableEvent.ipAddress
        audit.receivedAt = auditableEvent.receivedAt
        audit.httpStatus = auditableEvent.httpStatus
        audit.requestContent = auditableEvent.requestContent
        audit.caller = auditableEvent.caller
        audit.taskName = auditableEvent.taskName
        return audit.persistAndFlush<HttpNotarizationRequestAudit>()
            .onItem().invoke { record: HttpNotarizationRequestAudit -> bumpAuditMetric(record) }
            .onFailure().invoke { error: Throwable -> logAuditFailure(error) }.replaceWithVoid()
    }

    @WithTransaction
    fun pruneAuditLogs(): Uni<Long> {
        return HttpNotarizationRequestAudit.delete(
            "createdAt < ?1",
            OffsetDateTime.now().minus(auditLogsRetentionPeriod)
        )
            .onItem().invoke(Consumer<Long> { amountOfDeletedAuditLogs: Long ->
                registry.counter("http.audit.logs.prune").increment(amountOfDeletedAuditLogs.toDouble())
            })
    }

    private fun logAuditFailure(error: Throwable) {
        logger.error("Could not create audit entry", error)
    }

    private fun bumpAuditMetric(record: HttpNotarizationRequestAudit) {
        registry.counter(
            "audit.created",
            "action",
            record.action!!.name,
            "httpStatus",
            Integer.toString(record.httpStatus)
        )
    }

    companion object {
        private fun asStringId(id: SessionId?): String? {
            return id?.id
        }

        private fun asStringId(id: NotarizationRequestId?): String? {
            return id?.id?.toString()
        }
    }
}
