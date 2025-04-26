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

import eu.gaiax.notarization.request_processing.domain.model.*
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
class CallbackAuditingFilter(
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
        val resolvedRequest = request.get()
        try {
            val ipAddress = resolvedRequest.remoteAddress().hostAddress()
            val body = crc.getProperty(CACHE_REQUEST) as? Buffer
            val sessionId = resolvedRequest.params()[SESSION_ID]
            val event = OnAuditableHttp(
                UUID.randomUUID(),
                uriInfo.requestUri,
                asSessionId(sessionId),
                null,
                action,
                ipAddress,
                (crc.getProperty(RECEIVED_AT_TIME) as OffsetDateTime),
                body?.toString(),
                crc1.status,
                null,
                resolvedRequest.params()[TASK_NAME]
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
        private const val SESSION_ID = "eu.gaiax.notarization.request_processing.CallbackAuditingFilter.SESSION_ID"
        private const val TASK_NAME = "eu.gaiax.notarization.request_processing.CallbackAuditingFilter.TASK_NAME"
        @JvmStatic
        fun storeSessionId(request: HttpServerRequest, sessionId: String?) {
            request.params().add(SESSION_ID, sessionId)
        }

        @JvmStatic
        fun storeTaskName(request: HttpServerRequest, taskName: String?) {
            request.params().add(TASK_NAME, taskName)
        }

        private fun asSessionId(value: String?): SessionId? {
            return value?.let { SessionId(it) }
        }
    }
}
