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
package eu.gaiax.notarization.api.profile

import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.jose4j.jwk.PublicJsonWebKey

/**
 *
 * @author Neil Crossley
 */
class NotaryAccess(
    @get:Schema(
        description = "The encryption algorithm used when persisting encrypting identities to be read by a notary.",
        example = "A256GCM"
    ) val algorithm: String,
    @get:Schema(
        description = "The public JWK of a notary - https://www.rfc-editor.org/rfc/rfc7517",
        type = SchemaType.OBJECT
    ) val key: PublicJsonWebKey
) { }
