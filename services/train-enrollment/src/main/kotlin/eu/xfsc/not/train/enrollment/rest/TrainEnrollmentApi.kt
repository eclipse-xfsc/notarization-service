/****************************************************************************
 * Copyright 2024 ecsec GmbH
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
 ***************************************************************************/

package eu.xfsc.not.train.enrollment.rest

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.extensions.BeginTaskResponse
import eu.gaiax.notarization.api.extensions.ExtensionTaskServiceBlockingApi
import eu.xfsc.not.train.enrollment.ApplicationConfig
import eu.xfsc.not.train.enrollment.application.TrainEnrollmentController
import eu.xfsc.not.train.enrollment.rest.model.StartEnrollmentRequest
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.annotations.Operation
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * @author Mike Prechtl
 */
@Path("")
class TrainEnrollmentApi {

    companion object {
        const val NONCE_PARAM: String = "nonce"
        const val BEGIN_ENROLLMENT_TASK: String = "task/begin"
        const val START_ENROLLMENT: String = "task/{$NONCE_PARAM}/enrollment"
        const val CANCEL_ENROLLMENT_TASK: String = "task/{$NONCE_PARAM}/cancel"
    }

    @Inject
    lateinit var appConfig: ApplicationConfig

    @Inject
    lateinit var trainEnrollment: TrainEnrollmentController

    @Path(BEGIN_ENROLLMENT_TASK)
    fun beginTrainEnrollmentTask(
    ) : BeginNotarizationTaskResource {
        return BeginNotarizationTaskResource(trainEnrollment, appConfig)
    }

    class BeginNotarizationTaskResource(
        var trainEnrollment: TrainEnrollmentController,
        var appConfig: ApplicationConfig,
    ): ExtensionTaskServiceBlockingApi {
        @Operation(
            summary = "Begin a train enrollment task",
            description = "Begins train enrollment task"
        )
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Blocking
        override fun beginTask(
            @QueryParam("success") success: URI,
            @QueryParam("failure") failure: URI,
            @QueryParam("profileId") profileId: String,
            @QueryParam("taskName") taskName: String,
            data: JsonNode?,
        ): BeginTaskResponse {
            val nonce = trainEnrollment.beginTrainEnrollmentTask(success.toURL(), failure.toURL())
            return BeginTaskResponse(
                uriBuilder()
                    .path(START_ENROLLMENT)
                    .build(nonce),
                uriBuilder()
                    .path(CANCEL_ENROLLMENT_TASK)
                    .build(nonce)
            )
        }

        private fun uriBuilder() : UriBuilder {
            return appConfig.externalUrl()
                .map { UriBuilder.fromUri(it) }
                .orElseThrow {
                    logger.error { "There is no external train-enrollment URL configured." }
                    InternalServerErrorException("There is no external URL configured.")
                }
        }
    }

    @Operation(
        summary = "Cancel a train enrollment task",
        description = "Cancels a train enrollment task"
    )
    @Path(CANCEL_ENROLLMENT_TASK)
    @DELETE
    @Blocking
    fun cancelTrainEnrollmentTask(
        @PathParam(NONCE_PARAM) nonce: String,
    ) {
        trainEnrollment.cancelTrainEnrollment(nonce)
    }

    @Operation(
        summary = "Start train enrollment by creating a TSP",
        description = "Starts train enrollment by creating a TSP"
    )
    @Path(START_ENROLLMENT)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    fun startEnrollmentTSPCreation(
        @PathParam(NONCE_PARAM) nonce: String,
        startEnrollmentRequest: StartEnrollmentRequest,
    ) {
        trainEnrollment.createTSP(nonce, startEnrollmentRequest)
    }

}
