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
package eu.gaiax.notarization.request_processing.infrastructure.rest.client

import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileServiceHttpInterface
import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import io.quarkus.rest.client.reactive.ClientExceptionMapper
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

/**
 *
 * @author Neil Crossley
 */
@RegisterRestClient(configKey = "profile-api")
interface ProfileServiceRestClient: ProfileServiceHttpInterface {

    companion object {
        @ClientExceptionMapper
        fun toException(response: Response): NotFoundException? {
            return if (response.status == 404) {
                NotFoundException("The remote service responded with HTTP 404")
            } else null
        }
    }
}
