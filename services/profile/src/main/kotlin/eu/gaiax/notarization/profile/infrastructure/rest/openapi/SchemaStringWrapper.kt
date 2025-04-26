package eu.gaiax.notarization.profile.infrastructure.rest.openapi

import org.eclipse.microprofile.openapi.models.media.Schema

/**
 *
 * @author Neil Crossley
 */
class SchemaStringWrapper {
    private val wrapped: List<Schema?>

    constructor(wrapped: Schema?) {
        this.wrapped = listOf(wrapped)
    }

    constructor(wrapped: List<Schema?>) {
        this.wrapped = wrapped
    }

    override fun toString(): String {
        val builder: StringBuilder = StringBuilder()
        if (wrapped.size == 1) {
            append(builder, wrapped[0])
        } else {
            builder.append("[")
            append(builder, wrapped)
            builder.append("]")
        }
        return builder.toString()
    }

    private fun append(builder: StringBuilder, schemas: List<Schema?>) {
        var hasElement: Boolean = false
        for (item: Schema? in schemas) {
            hasElement = addSeperator(builder, hasElement)
            append(builder, item)
        }
    }

    private fun append(builder: StringBuilder, schema: Schema?) {
        builder.append("[")
        var hasAdded: Boolean = false
        if (schema!!.description != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("description:")
            builder.append(schema.description)
        }
        if (schema.title != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("title:")
            builder.append(schema.title)
        }
        if (schema.ref != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("ref:")
            builder.append(schema.ref)
        }
        if (schema.type != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("type:")
            builder.append(schema.type)
        }
        if (schema.items != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("items:")
            append(builder, schema.items)
        }
        if (schema.nullable != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("nullable:")
            builder.append(schema.nullable)
        }
        if (schema.allOf != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("allOf:")
            append(builder, schema.allOf)
        }
        if (schema.anyOf != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("anyOf:")
            append(builder, schema.anyOf)
        }
        if (schema.oneOf != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("oneOf:")
            append(builder, schema.oneOf)
        }
        if (schema!!.example != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("example:")
            builder.append(schema.example)
        }
        if (schema.additionalPropertiesSchema != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("additionalProperties:")
            append(builder, schema.additionalPropertiesSchema)
        }
        builder.append("]")
    }

    private fun addSeperator(builder: StringBuilder, hasAppended: Boolean): Boolean {
        if (hasAppended) {
            builder.append(";")
        }
        return true
    }
}
