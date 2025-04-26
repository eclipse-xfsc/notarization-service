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
package eu.gaiax.notarization.request_processing.domain.model.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import java.io.IOException

/**
 *
 * @author Neil Crossley
 */
class NotarizationRequestIdSerializer : JsonSerializer<NotarizationRequestId?>() {
    @Throws(IOException::class)
    override fun serialize(t: NotarizationRequestId?, jg: JsonGenerator, sp: SerializerProvider) {
        jg.writeString(t?.id.toString())
    }
}
