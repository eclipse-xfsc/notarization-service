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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import eu.gaiax.notarization.request_processing.domain.model.serialization.DocumentHashDeserializer
import eu.gaiax.notarization.request_processing.domain.model.serialization.DocumentHashSerializer
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Florian Otto
 */
@Schema(type = SchemaType.STRING)
@JsonDeserialize(using = DocumentHashDeserializer::class)
@JsonSerialize(using = DocumentHashSerializer::class)
class DocumentHash(val value: String) {
    companion object {
        fun valueOf(hash: DocumentHash?): String? {
            return hash?.value
        }
    }
    override fun toString(): String {
        return value
    }
}
