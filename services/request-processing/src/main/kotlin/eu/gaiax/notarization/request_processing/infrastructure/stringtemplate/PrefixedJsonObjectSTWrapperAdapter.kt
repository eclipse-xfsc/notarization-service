package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ModelAdaptor
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.misc.STNoSuchPropertyException

class PrefixedJsonObjectSTWrapperAdapter : ModelAdaptor<PrefixedJsonObjectSTWrapper> {

    override fun getProperty(
        interp: Interpreter?,
        self: ST?,
        model: PrefixedJsonObjectSTWrapper?,
        property: Any?,
        propertyName: String?
    ): Any {
        val jsonModel = model?.json
        if (jsonModel != null && propertyName != null) {
            if (!propertyName.endsWith(".")) {

                val currentPrefix = "${model.prefix}$propertyName"
                if (jsonModel.has(currentPrefix)) {
                    val found = jsonModel.get(currentPrefix)
                    if (found is ObjectNode) {
                        return JsonObjectSTWrapper(found)
                    }
                    if (found is ArrayNode) {
                        return JsonArraySTWrapper(found)
                    }
                    return found.toString()
                } else {
                    val targetPrefix = "$currentPrefix."
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
