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

package eu.xfsc.not.compliance.application

import eu.xfsc.not.compliance.entities.Session
import eu.xfsc.not.compliance.rest.client.ComplianceService
import eu.xfsc.not.compliance.rest.client.NotarizationCallback
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.json.JsonObjectBuilder
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.RestClientBuilder
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.ClientWebApplicationException
import java.net.URL
import java.security.SecureRandom
import java.util.*


private val logger = KotlinLogging.logger {}


/**
 * @author Mike Prechtl
 */
@ApplicationScoped
class ComplianceManager {

    companion object {
        const val DEFAULT_NONCE_SIZE_BYTES : Int = 24
    }

    @RestClient
    lateinit var complianceService: ComplianceService

    @Transactional
    fun startComplianceTask(success: URL, failure: URL): String {
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
    fun cancelComplianceTask(nonce: String) {
        val deletedSessions = Session.delete("nonce", nonce)
        if (deletedSessions == 0L) {
            throw NotFoundException("Session not found or already expired.")
        } else {
            logger.debug { "Canceled compliance task with the nonce $nonce." }
        }
    }

    @Transactional
    fun submitVerifiablePresentation(nonce: String, vp: JsonObject) {
        val session = Session.findByNonceOptional(nonce)
            ?: throw NotFoundException("Session not found, expired or already used.")

        try {
            val verifiableCredential = complianceService.checkCompliance(null, vp)
            val successCallbackReq = buildCallbackJsonObject(vp, verifiableCredential, null)
            RestClientBuilder.newBuilder()
                .baseUrl(session.successURL)
                .build(NotarizationCallback::class.java)
                .finishTask(successCallbackReq)
        } catch (ex: ClientWebApplicationException) {
            val error = ex.response.readEntity(JsonObject::class.java)
            val failureCallbackReq = buildCallbackJsonObject(vp,null, error)
            RestClientBuilder.newBuilder()
                .baseUrl(session.failureURL)
                .build(NotarizationCallback::class.java)
                .finishTask(failureCallbackReq)
        }
        session.delete()
    }

    private fun createNonce() : String {
        val bytes = ByteArray(DEFAULT_NONCE_SIZE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().encodeToString(bytes)
    }

    private fun buildCallbackJsonObject(vp: JsonObject, vc: JsonObject?, error: JsonObject?) : JsonObject {
        val builder: JsonObjectBuilder = Json.createObjectBuilder()
        builder.add("verifiablePresentation", vp)
        vc?.let { builder.add("verifiableCredential", it) }
        error?.let { builder.add("error", it) }
        return builder.build()
    }
}
