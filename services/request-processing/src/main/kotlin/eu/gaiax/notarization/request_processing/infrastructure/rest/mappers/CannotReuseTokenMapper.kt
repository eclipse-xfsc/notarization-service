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
package eu.gaiax.notarization.request_processing.infrastructure.rest.mappers

import eu.gaiax.notarization.request_processing.domain.exception.CannotReuseTokenException
import eu.gaiax.notarization.request_processing.infrastructure.rest.mappers.problem_details.ProblemDetails
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import mu.KotlinLogging
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@Provider
class CannotReuseTokenMapper {
    object Details {
        const val TITLE = "Cannot Re-use Token"
    }

    @ServerExceptionMapper
    fun mapException(x: CannotReuseTokenException?): RestResponse<ProblemDetails> {
        logger.debug("Handled token reuse exception", x)
        return RestResponse.status(Response.Status.BAD_REQUEST, toDetails())
    }

    companion object {
        private fun toDetails(): ProblemDetails {
            return ProblemDetails(
                "Cannot Re-use Token",
                Response.Status.BAD_REQUEST.statusCode,
                "Provided token has already been used",
                null
            )
        }
    }
}
