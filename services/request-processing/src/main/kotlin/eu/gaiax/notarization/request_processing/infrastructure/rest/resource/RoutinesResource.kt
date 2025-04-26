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
package eu.gaiax.notarization.request_processing.infrastructure.rest.resource

import eu.gaiax.notarization.request_processing.application.AuditService
import eu.gaiax.notarization.request_processing.application.NotarizationRequestStore
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.reactive.ResponseStatus

private val log = KotlinLogging.logger {}

/**
 *
 * @author Florian Otto
 */
@Tag(name = Api.Tags.ROUTINES)
@Path(Api.Path.ROUTINES_RESOURCE)
@PermitAll
@SecurityRequirements
class RoutinesResource(
    private val notarizationRequestStore: NotarizationRequestStore
) {
    @Inject
    lateinit var auditService: AuditService
    @Auditable(action = NotarizationRequestAction.PRUNE_TERMINATED_SESSIONS)
    @POST
    @Path(Api.Path.ROUTINES_RESOURCE_PRUNE_TERMINATED_SESS)
    @Operation(
        summary = "Prune terminated/issued sessions",
        description = "Will delete terminated sessions and sessions which were completed succesfully completely."
    )
    fun pruneTerminatedSessions(): Uni<Void> {
        return notarizationRequestStore.pruneTerminatedSessions().invoke { results ->
            if (results > 0) {
                log.debug { "Prune terminated/issued sessions called affecting total $results." }
            }
        }.replaceWithVoid()
    }

    @Auditable(action = NotarizationRequestAction.PRUNE_TIMEOUT_SESSIONS)
    @POST
    @Path(Api.Path.ROUTINES_RESOURCE_PRUNE_TIMEOUT_SESS)
    @ResponseStatus(204)
    @Operation(summary = "Prune timed out sessions", description = "Will delete timed out sessions completely.")
    fun pruneTimeoutSessions(): Uni<Void> {
        return notarizationRequestStore.pruneTimeoutSessions().invoke { results ->
            if (results > 0) {
                log.debug { "Prune timed out sessions called affecting total $results." }
            }
        }.replaceWithVoid()
    }

    @Auditable(action = NotarizationRequestAction.PRUNE_SUBMISSIONTIMEOUT_SESSIONS)
    @POST
    @Path(Api.Path.ROUTINES_RESOURCE_PRUNE_SUBMISIION_TIMEOUT_SESS)
    @ResponseStatus(204)
    @Operation(
        summary = "Prune terminated Sessions",
        description = "Will delete sessions without submissions after timeout completely."
    )
    fun pruneSubmissTimeoutSessions(): Uni<Void> {
        return notarizationRequestStore.pruneSubmissionTimeoutSessions().invoke { results ->
            if (results > 0) {
                log.debug { "Prune sub timeout sessions called affecting total $results." }
            }
        }.replaceWithVoid()
    }

    @POST
    @Path(Api.Path.ROUTINES_RESOURCE_PRUNE_AUDIT_LOGS)
    @ResponseStatus(204)
    @Operation(
        summary = "Prune audit logs",
        description = "Will delete http audit logs after a given retention period."
    )
    fun pruneAuditLogs(): Uni<Void> {
        return auditService.pruneAuditLogs().invoke { results ->
            if (results > 0) {
                log.debug { "Prune audit logs affecting total $results." }
            }
        }.replaceWithVoid()
    }
}
