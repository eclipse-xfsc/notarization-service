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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsonp.JSONPModule
import io.quarkus.jackson.ObjectMapperCustomizer
import jakarta.ws.rs.ext.Provider
import org.jose4j.jwk.PublicJsonWebKey

/**
 *
 * @author Neil Crossley
 */
@Provider
class EnhanceSerializationCustomizer : ObjectMapperCustomizer {
    override fun customize(objectMapper: ObjectMapper) {
        objectMapper.registerModule(JSONPModule())
        val module = SimpleModule()
        module.addSerializer(PublicJsonWebKeySerializer())
        module.addDeserializer(PublicJsonWebKey::class.java, PublicJsonWebKeyDeserializer())
        objectMapper.registerModule(module)
    }
}
