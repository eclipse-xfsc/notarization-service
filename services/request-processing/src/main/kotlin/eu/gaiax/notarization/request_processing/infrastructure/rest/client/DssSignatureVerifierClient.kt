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

import eu.gaiax.notarization.request_processing.domain.services.SignatureVerifierService
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.DssRestClient.SignedDocument
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.DssRestClient.ValidateSignatureRequest
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.JsonValue
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.io.IOException
import java.io.InputStream
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Michael Rauh
 */
@ApplicationScoped
class DssSignatureVerifierClient(@param:RestClient val dssClient: DssRestClient) : SignatureVerifierService {
    @Inject
    lateinit var dssPolicy: DssPolicyConfig
    override fun verify(content: InputStream): Uni<ByteArray> {
        val request = ValidateSignatureRequest()

        // Only apply a custom policy if present.
        dssPolicy.content().ifPresent { c: String? ->
            val requestPolicy = DssRestClient.Policy()
            requestPolicy.bytes = c
            request.policy = requestPolicy
        }
        val fileEncoded = try {
            Base64.getEncoder().encodeToString(content.readAllBytes())
        } catch (ex: IOException) {
            logger.warn("Cannot read bytes from InputStream", ex)
            throw IllegalArgumentException(ex)
        }
        val signedDocument = SignedDocument()
        signedDocument.bytes = fileEncoded
        signedDocument.name = "xades-detached.xml"
        request.signedDocument = signedDocument
        val dssReports = dssClient.validateSignature(request)
        return dssReports.map { givenReports: JsonValue ->
            val reports = givenReports.asJsonObject()
            val etsiReportEncoded = reports["validationReportDataHandler"]
            if (etsiReportEncoded == null) {
                logger.error("The ETSI report was unexpectedly missing")
                null
            } else {
                val etsiReport = Base64.getMimeDecoder().decode(etsiReportEncoded.toString())
                logger.debug { "Received ETSI report: $etsiReport" }
                etsiReport
            }
        }
    }

}
