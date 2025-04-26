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
package eu.gaiax.notarization.request_processing.application

import eu.gaiax.notarization.request_processing.application.domain.VerificationResult
import eu.gaiax.notarization.request_processing.domain.services.SignatureVerifierService
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class VerificationService(
    private val signatureVerifier: SignatureVerifierService,
    private val logger: Logger
) {
    @WithTransaction
    fun verify(bytes: ByteArray?): Uni<VerificationResult> {
        return verify(ByteArrayInputStream(bytes))
    }

    @WithTransaction
    fun verify(`is`: InputStream): Uni<VerificationResult> {
        val digestAlgorithm = "SHA3-256"
        val md = try {
            MessageDigest.getInstance(digestAlgorithm)
        } catch (ex: NoSuchAlgorithmException) {
            throw IllegalArgumentException("Illegal digest", ex)
        }
        val dis = DigestInputStream(`is`, md)
        return signatureVerifier.verify(dis)
            .onTermination().invoke(Runnable {
                try {
                    dis.close()
                } catch (ex: IOException) {
                    logger.warn("Could not close the digest input stream", ex)
                }
                try {
                    `is`.close()
                } catch (ex: IOException) {
                    logger.warn("Could not close the file input stream", ex)
                }
            }).map { rawReport: ByteArray ->
                val digest = md.digest()
                VerificationResult(digestAlgorithm, digest, rawReport)
            }
    }
}
