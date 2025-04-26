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

import mu.KotlinLogging
import org.eclipse.microprofile.config.spi.Converter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Michael Rauh
 */
class OptionalFileToBase64Converter : Converter<String?> {
    override fun convert(path: String?): String? {
        if (path.isNullOrEmpty()) {
            return null
        }
        val customPolicy = javaClass.getResourceAsStream(path)
        return try {
            val policyAsString = String(customPolicy!!.readAllBytes(), StandardCharsets.UTF_8)
            String(Base64.getEncoder().encode(policyAsString.toByteArray()))
        } catch (ex: IOException) {
            logger.error { "Cannot read content of provided file $path" }
            throw IllegalStateException(ex)
        }
    }

}
