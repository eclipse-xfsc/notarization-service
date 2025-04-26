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
import io.quarkus.arc.DefaultBean
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
@DefaultBean
class StubSignatureVerifierClient(
    @param:RestClient private val dssClient: DssRestClient
) : SignatureVerifierService {
    override fun verify(content: InputStream): Uni<ByteArray> {
        try {
            content.readAllBytes()
        } catch (ex: IOException) {
            return Uni.createFrom().failure(ex)
        }
        return Uni.createFrom().item(UUID.randomUUID().toString().toByteArray())
    }
}
