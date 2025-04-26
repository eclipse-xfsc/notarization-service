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

package eu.xfsc.not.train.enrollment.application

import com.fasterxml.jackson.databind.JsonNode
import eu.xfsc.not.train.enrollment.entities.Session
import eu.xfsc.not.train.enrollment.rest.clients.NotarizationCallback
import eu.xfsc.not.train.enrollment.rest.clients.TrainService
import eu.xfsc.not.train.enrollment.rest.model.CallbackRequest
import eu.xfsc.not.train.enrollment.rest.model.StartEnrollmentRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.RestClientBuilder
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.ClientWebApplicationException
import java.net.URI
import java.net.URL
import java.security.SecureRandom
import java.util.Base64


private val logger = KotlinLogging.logger {}


/**
 * @author Mike Prechtl
 */
@ApplicationScoped
class TrainEnrollmentController {

    companion object {
        const val DEFAULT_NONCE_SIZE_BYTES : Int = 24
    }

    @RestClient
    lateinit var trainEnrollmentService: TrainService

    @Transactional
    fun beginTrainEnrollmentTask(success: URL, failure: URL): String {
        val sessionId = ellog.uuid.UUID.createRandom()
        val nonce = createNonce()

        val session = Session()
        session.id = sessionId.toString()
        session.nonce = nonce
        session.successURL = success
        session.failureURL = failure
        session.persistAndFlush()

        return session.nonce
    }

    @Transactional
    fun cancelTrainEnrollment(nonce: String) {
        val deletedSessions = Session.delete("nonce", nonce)
        if (deletedSessions == 0L) {
            throw NotFoundException("Session not found or already expired.")
        } else {
            logger.debug { "Canceled train enrollment task with the nonce $nonce." }
        }
    }

    @Transactional
    fun createTSP(nonce: String, startEnrollmentRequest: StartEnrollmentRequest) {
        val session = Session.findByNonceOptional(nonce)
            ?: throw NotFoundException("Session not found, expired or already used.")

        val frameworkName = startEnrollmentRequest.frameworkName
        val trustListEndpoint = startEnrollmentRequest.trustListEndpoint

        if (frameworkName != null && trustListEndpoint != null) {
            throw BadRequestException("You have either to provide a frameworkName or a trustListEndpoint, but not both.")
        }

        if (frameworkName == null && trustListEndpoint == null) {
            throw BadRequestException("You have either to provide a frameworkName or a trustListEndpoint.")
        }

        try {
            createTSPAtTrain(frameworkName, trustListEndpoint, startEnrollmentRequest.tspJson)
            val successCallbackReq = CallbackRequest(startEnrollmentRequest, null)
            RestClientBuilder.newBuilder()
                .baseUrl(session.successURL)
                .build(NotarizationCallback::class.java)
                .finishTask(successCallbackReq)
        } catch (ex: ClientWebApplicationException) {
            val error = ex.response.readEntity(String::class.java)
            val failureCallbackReq = CallbackRequest(null, error)
            RestClientBuilder.newBuilder()
                .baseUrl(session.failureURL)
                .build(NotarizationCallback::class.java)
                .finishTask(failureCallbackReq)
        }

        session.delete()
    }

    @Throws(ClientWebApplicationException::class)
    private fun createTSPAtTrain(frameworkName: String?, trustListEndpoint: URI?, tspJson: JsonNode) {
        if (frameworkName != null) {
            trainEnrollmentService.createTSP(
                frameworkName,
                tspJson
            )
        } else {
            RestClientBuilder.newBuilder()
                .baseUri(trustListEndpoint)
                .build(TrainService::class.java)
                .createTSPViaProvidedTrustListEndpoint(tspJson)
        }
    }

    private fun createNonce() : String {
        val bytes = ByteArray(DEFAULT_NONCE_SIZE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().encodeToString(bytes)
    }
}
