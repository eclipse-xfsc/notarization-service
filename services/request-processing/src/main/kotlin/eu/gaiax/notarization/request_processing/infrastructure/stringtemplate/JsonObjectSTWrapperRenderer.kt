/*
 *
 */
package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import org.stringtemplate.v4.AttributeRenderer
import java.util.*

/**
 *
 * @author Neil Crossley
 */
class JsonObjectSTWrapperRenderer : AttributeRenderer<JsonObjectSTWrapper> {
    override fun toString(wrapper: JsonObjectSTWrapper, formatString: String?, locale: Locale): String? {
        val model = wrapper.json
        return model.toString()
    }
}
