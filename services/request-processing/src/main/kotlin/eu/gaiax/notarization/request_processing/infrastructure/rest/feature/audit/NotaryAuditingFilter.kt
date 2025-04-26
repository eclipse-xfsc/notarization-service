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
package eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit

import eu.gaiax.notarization.request_processing.domain.model.Channel
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import eu.gaiax.notarization.request_processing.domain.model.OnAuditableHttp
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerRequest
import io.vertx.mutiny.core.eventbus.EventBus
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import java.io.IOException
import java.time.OffsetDateTime
import java.util.*

/**
 *
 * @author Neil Crossley
 */
class NotaryAuditingFilter(
    private val action: NotarizationRequestAction,
    private val bus: Instance<EventBus>,
    private val request: Instance<HttpServerRequest>
) : ContainerResponseFilter, ContainerRequestFilter {
    private val auditRequestContent: Boolean = !NotarizationRequestAction.NonAuditableContent.contains(action)

    @Throws(IOException::class)
    override fun filter(crc: ContainerRequestContext) {
        crc.setProperty(RECEIVED_AT_TIME, OffsetDateTime.now())
        val resolvedRequest = request.get()
        try {
            if (auditRequestContent) {
                resolvedRequest.bodyHandler { buffer: Buffer? -> crc.setProperty(CACHE_REQUEST, buffer) }
            } else {
                // HACK: force the loading/initialization of the proxied instance.
                resolvedRequest.absoluteURI()
            }
        } finally {
            request.destroy(resolvedRequest)
        }
    }

    @Throws(IOException::class)
    override fun filter(crc: ContainerRequestContext, crc1: ContainerResponseContext) {
        val uriInfo = crc.uriInfo
        val pathParameters = uriInfo.pathParameters
        val resolvedRequest = request.get()
        try {
            val ipAddress = resolvedRequest.remoteAddress().hostAddress()
            val body = crc.getProperty(CACHE_REQUEST) as? Buffer
            val event = OnAuditableHttp(
                UUID.randomUUID(),
                uriInfo.requestUri,
                null,
                asRequestId(pathParameters.getFirst(Api.Param.NOTARIZATION_REQUEST_ID)),
                action,
                ipAddress,
                (crc.getProperty(RECEIVED_AT_TIME) as? OffsetDateTime ?: OffsetDateTime.now()),
                body?.toString(),
                crc1.status,
                resolveCaller(crc),
                null
            )
            notify(event)
        } finally {
            request.destroy(resolvedRequest)
        }
    }

    private fun notify(event: OnAuditableHttp) {
        val resolvedBus = bus.get()
        try {
            resolvedBus.requestAndForget<Any>(Channel.ON_AUDITABLE, event)
        } finally {
            bus.destroy(resolvedBus)
        }
    }

    companion object {
        private const val CACHE_REQUEST = "AUDIT_CACHE_REQUEST"
        private const val RECEIVED_AT_TIME = "AUDIT_RECEIVED_REQUEST"
        private fun resolveCaller(crc: ContainerRequestContext): String? {
            val securityContext = crc.securityContext ?: return null
            val userPrincipal = securityContext.userPrincipal ?: return null
            return userPrincipal.name
        }

        private fun asRequestId(value: String?): NotarizationRequestId? {
            return value?.let { NotarizationRequestId(it) }
        }
    }
}
