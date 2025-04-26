/*
 *
 */
package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 *
 * @author Neil Crossley
 */
class JsonArraySTWrapper internal constructor(val json: ArrayNode) : Iterable<Any?> {
    override fun iterator(): Iterator<Any> {
        return IterStWrapper(json, json.iterator())
    }

    companion object {
        class IterStWrapper(val json: ArrayNode, val iterator: Iterator<JsonNode>): Iterator<Any> {

            override fun hasNext(): Boolean {
                return iterator.hasNext();
            }

            override fun next(): Any {
                var current = iterator.next();
                if (current is ObjectNode) {
                    return JsonObjectSTWrapper(current);
                }
                if (current is ArrayNode) {
                    return JsonArraySTWrapper(current);
                }
                return current;
            }

            override fun toString(): String {
                return this.json.toString();
            }
        }
    }
}

