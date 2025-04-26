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
package eu.gaiax.notarization.request_processing.domain.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 *
 * @author Florian Otto
 */
@Converter
class StringConverter : AttributeConverter<String, ByteArray> {
    override fun convertToDatabaseColumn(attribute: String?): ByteArray? {
        return attribute?.toByteArray() ?: ByteArray(0)
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): String? {
        return dbData?.let { String(it) }
    }
}
