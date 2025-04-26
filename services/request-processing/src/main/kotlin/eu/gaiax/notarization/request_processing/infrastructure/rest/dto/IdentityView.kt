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
package eu.gaiax.notarization.request_processing.infrastructure.rest.dto

import eu.gaiax.notarization.request_processing.domain.entity.RequestorIdentity
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.OffsetDateTime

/**
 *
 * @author Neil Crossley
 */
class IdentityView {
    @get:Schema(required = true, description = "The encrypted identity.")
    var data: EncryptedData? = null

    @get:Schema(required = true, description = "The algorithm used to encrypt the identity.")
    var algorithm: String? = null
    var encryption: String? = null
    var jwk: String? = null

    @get:Schema(readOnly = true)
    @NotNull var createdAt: OffsetDateTime? = null

    companion object {
        @kotlin.jvm.JvmStatic
        fun from(reqId: RequestorIdentity): IdentityView {
            val iv = IdentityView()
            iv.algorithm = reqId.algorithm
            iv.createdAt = reqId.createdAt
            iv.data = EncryptedData(reqId.data!!)
            iv.encryption = reqId.encryption
            iv.jwk = reqId.jwk
            return iv
        }
    }
}
