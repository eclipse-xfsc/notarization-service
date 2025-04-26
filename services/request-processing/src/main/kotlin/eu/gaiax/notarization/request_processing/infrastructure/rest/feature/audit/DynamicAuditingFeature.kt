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

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import io.vertx.core.http.HttpServerRequest
import io.vertx.mutiny.core.eventbus.EventBus
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.container.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.ext.Provider

/**
 *
 * @author Neil Crossley
 */
@Provider
class DynamicAuditingFeature : DynamicFeature {
    @Inject
    lateinit var bus: Instance<EventBus>

    @Context
    lateinit var request: Instance<HttpServerRequest>
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        if (resourceInfo.resourceMethod.isAnnotationPresent(Auditable::class.java)) {
            val annotation = resourceInfo.resourceMethod.getAnnotation(Auditable::class.java)
            val action: NotarizationRequestAction = annotation.action
            if (NotarizationRequestAction.RequestorActions.contains(action)) {
                context.register(RequestorAuditingFilter(annotation.action, bus, request), 1)
            } else if (NotarizationRequestAction.NotaryActions.contains(action)) {
                context.register(NotaryAuditingFilter(annotation.action, bus, request), 1)
            } else if (NotarizationRequestAction.CallbackActions.contains(action)) {
                context.register(CallbackAuditingFilter(annotation.action, bus, request), 1)
            }
        }
    }
}
