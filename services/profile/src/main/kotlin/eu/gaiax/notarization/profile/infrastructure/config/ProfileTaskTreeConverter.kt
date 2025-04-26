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
package eu.gaiax.notarization.profile.infrastructure.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.spi.Converter

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class ProfileTaskTreeConverter : Converter<ProfileTaskTree> {
    var mapper: ObjectMapper = jacksonObjectMapper()

    @Throws(IllegalArgumentException::class, NullPointerException::class)
    override fun convert(value: String): ProfileTaskTree {
        return try {
            mapper.readValue(value, ProfileTaskTree::class.java)
        } catch (ex: JsonProcessingException) {
            throw IllegalArgumentException(ex)
        }
    }
}
