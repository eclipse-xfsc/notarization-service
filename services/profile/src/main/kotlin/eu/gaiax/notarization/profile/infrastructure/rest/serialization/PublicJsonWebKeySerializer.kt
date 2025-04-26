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
package eu.gaiax.notarization.profile.infrastructure.rest.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.jose4j.jwk.PublicJsonWebKey
import java.io.IOException

/**
 *
 * @author Neil Crossley
 */
class PublicJsonWebKeySerializer : StdSerializer<PublicJsonWebKey>(
    PublicJsonWebKey::class.java
) {
    @Throws(IOException::class)
    override fun serialize(t: PublicJsonWebKey, jg: JsonGenerator, sp: SerializerProvider) {
        jg.writeRawValue(t.toJson())
    }
}
