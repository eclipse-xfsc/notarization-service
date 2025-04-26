package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ModelAdaptor
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.misc.STNoSuchPropertyException

class JsonObjectSTWrapperAdapter : ModelAdaptor<JsonObjectSTWrapper> {

    override fun getProperty(
        interp: Interpreter?,
        self: ST?,
        model: JsonObjectSTWrapper?,
        property: Any?,
        propertyName: String?
    ): Any {
        val jsonModel = model?.json
        if (jsonModel != null && propertyName != null) {
            if (jsonModel.has(propertyName)) {
                val found = jsonModel.get(propertyName)
                if (found is ObjectNode) {
                    return JsonObjectSTWrapper(found)
                }
                if (found is ArrayNode) {
                    return JsonArraySTWrapper(found)
                }

                return found.toString()
            } else {
                if (!propertyName.endsWith(".")) {

                    val targetPrefix = "$propertyName."
                    val hasPrefix = jsonModel.fieldNames().asSequence().any { it.startsWith(targetPrefix) }

                    if (hasPrefix) {
                        return PrefixedJsonObjectSTWrapper(jsonModel, targetPrefix)
                    }
                }
            }
        }

        throw STNoSuchPropertyException(null, jsonModel, propertyName)
    }
}
