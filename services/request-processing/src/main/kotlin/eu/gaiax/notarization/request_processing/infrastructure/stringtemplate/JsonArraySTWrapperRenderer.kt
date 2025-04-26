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
class JsonArraySTWrapperRenderer : AttributeRenderer<JsonArraySTWrapper> {
    override fun toString(wrapper: JsonArraySTWrapper, formatString: String?, locale: Locale): String {
        val model = wrapper.json
        return model.toString()
    }
}
