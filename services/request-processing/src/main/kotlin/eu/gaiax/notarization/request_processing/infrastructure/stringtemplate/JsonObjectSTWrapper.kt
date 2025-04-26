/*
 *
 */
package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 *
 * @author Neil Crossley
 */
class JsonObjectSTWrapper(@JvmField var json: ObjectNode) {
    companion object {
        fun from(json: ObjectNode?): JsonObjectSTWrapper? {
            return json?.let { JsonObjectSTWrapper(it) }
        }

        @JvmStatic
        fun from(json: String?): JsonObjectSTWrapper? {
            return try {
                if (json == null) null else JsonObjectSTWrapper(ObjectMapper().readTree(json) as ObjectNode)
            } catch (ex: Exception) {
                throw IllegalStateException("Failed to parse json: $json", ex)
            }
        }
    }
}
