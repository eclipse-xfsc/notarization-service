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
package eu.gaiax.notarization.request_processing.infrastructure.rest.feature.rbac

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit.Auditable
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.*
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.ext.Provider

/**
 *
 * @author Neil Crossley
 */
@Provider
class DynamicRbacFeature : DynamicFeature {
    @Inject
    lateinit var registry: MeterRegistry

    @Inject
    lateinit var profileService: ProfileService

    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        if (resourceInfo.resourceMethod.isAnnotationPresent(Auditable::class.java)) {
            val annotation = resourceInfo.resourceMethod.getAnnotation(Auditable::class.java)
            val action: NotarizationRequestAction = annotation.action
            if (roleSpecificNotaryAction.contains(action)) {
                context.register(
                    PathParameterRbacFilter(
                        action,
                        Api.Param.PROFILE_ID,
                        registry,
                        profileService
                    ),
                    Priorities.AUTHORIZATION
                )
            }
        }
    }

    companion object {
        private val roleSpecificNotaryAction: MutableSet<NotarizationRequestAction>

        init {
            roleSpecificNotaryAction = HashSet(NotarizationRequestAction.NotaryActions)
            roleSpecificNotaryAction.remove(NotarizationRequestAction.NOTARY_FETCH_ALL)
            roleSpecificNotaryAction.remove(NotarizationRequestAction.NOTARY_REVOKE)
        }
    }
}
