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
package eu.gaiax.notarization.request_processing.domain.model

import eu.gaiax.notarization.api.profile.NotaryAccess
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwe.JsonWebEncryption
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.lang.JoseException

/**
 *
 * @author Neil Crossley
 */
@Throws(JoseException::class)
fun NotaryAccess.encrypt(encryption: String?, content: String?): String {
    val senderJwe = JsonWebEncryption()
    senderJwe.setContentEncryptionAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
    senderJwe.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
    senderJwe.setPlaintext(content)
    senderJwe.algorithmHeaderValue = algorithm
    senderJwe.encryptionMethodHeaderParameter = encryption
    senderJwe.key = this.key.key
    senderJwe.keyIdHeaderValue = this.key.keyId
    return senderJwe.compactSerialization
}
