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
class PrefixedJsonObjectSTWrapper(@JvmField var json: ObjectNode, var prefix: String) {

}
