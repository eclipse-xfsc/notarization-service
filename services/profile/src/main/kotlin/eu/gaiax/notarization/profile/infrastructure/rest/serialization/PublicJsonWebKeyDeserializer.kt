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

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.lang.JoseException
import java.io.IOException

/**
 *
 * @author Neil Crossley
 */
class PublicJsonWebKeyDeserializer : StdDeserializer<PublicJsonWebKey>(
    PublicJsonWebKey::class.java
) {
    @Throws(IOException::class, JacksonException::class)
    override fun deserialize(jp: JsonParser, dc: DeserializationContext): PublicJsonWebKey {
        val rawJson: Map<String, Object> = jp.readValueAs(object: TypeReference<Map<String, Object>>() {});
        return try {
            PublicJsonWebKey.Factory.newPublicJwk(rawJson)
        } catch (ex: JoseException) {
            throw IllegalArgumentException(ex)
        }
    }
}
