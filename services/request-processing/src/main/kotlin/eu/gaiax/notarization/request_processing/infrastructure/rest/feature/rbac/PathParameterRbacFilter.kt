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

import eu.gaiax.notarization.request_processing.domain.exception.AuthorizationException
import eu.gaiax.notarization.request_processing.domain.exception.ForbiddenException
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import io.micrometer.core.instrument.MeterRegistry
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter
import java.io.IOException
import java.time.Duration
import java.util.function.Consumer

/**
 *
 * @author Neil Crossley
 */
class PathParameterRbacFilter(
    val action: NotarizationRequestAction,
    val pathParameterName: String,
    val registry: MeterRegistry,
    val profileService: ProfileService,
) : ResteasyReactiveContainerRequestFilter {

    val waitTimeout = Duration.ofSeconds(1)

    @Throws(IOException::class)
    override fun filter(crc: ResteasyReactiveContainerRequestContext) {
        val securityContext = crc.securityContext
        val user = securityContext.userPrincipal
        val pathValue = crc.uriInfo.pathParameters.getFirst(pathParameterName)
        if (user == null) {
            throw AuthorizationException(
                "Missing authentication", SessionId(
                    pathValue!!
                )
            )
        }
        if (pathValue == null) {
            throw AuthorizationException(
                "Missing value", null
            )
        }
        crc.suspend()
        profileService.find(ProfileId(pathValue))
            .map { profile ->
                if (profile == null) {
                    throw AuthorizationException(
                        "Missing value", null
                    )

                }
                val hasRole = profile.notaryRoles.any { securityContext.isUserInRole(it) }
                if (!hasRole) {
                    registry.counter("rbac.authentication.failure", "action", action.name, "role", pathValue).increment()
                    throw ForbiddenException("Forbidden", SessionId(pathValue))
                }
                registry.counter("rbac.authentication.success", "action", action.name, "role", pathValue).increment()
            }
            .subscribe().with({
                crc.resume()
        }, ResumeThrowableConsumer(crc))

        /*
        val foundProfile = profileService.findBlocking(ProfileId(pathValue))
            ?: throw AuthorizationException(
                "Missing value", null
            )
         */

    }

    companion object {
        internal class ResumeThrowableConsumer(private val context: ResteasyReactiveContainerRequestContext) :
            Consumer<Throwable?> {
            override fun accept(throwable: Throwable?) {
                context.resume(throwable)
            }
        }
    }
}
