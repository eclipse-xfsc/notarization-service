package eu.gaiax.notarization.profile.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import org.eclipse.microprofile.config.spi.Converter
import java.util.*

private val logger = KotlinLogging.logger {}

class ObjectNodeConverter : Converter<ObjectNode> {
    override fun convert(value: String?): ObjectNode? {
        return if (value.isNullOrBlank()) null else ObjectMapper().readTree(value) as ObjectNode
    }
}

class JsonArrayConverter : Converter<ArrayNode> {
    override fun convert(value: String): ArrayNode {
        return if (value.isEmpty()) {
            ObjectMapper().createArrayNode()
        } else {
            ObjectMapper().readTree(value) as ArrayNode
        }
    }
}
