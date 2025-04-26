package eu.gaiax.notarization.request_processing.infrastructure.rest.openapi

import org.eclipse.microprofile.openapi.models.media.Schema

/**
 *
 * @author Neil Crossley
 */
class SchemaStringWrapper {
    private val wrapped: List<Schema?>

    constructor(wrapped: Schema?) {
        this.wrapped = java.util.List.of(wrapped)
    }

    constructor(wrapped: List<Schema?>) {
        this.wrapped = wrapped
    }

    public override fun toString(): String {
        val builder: StringBuilder = StringBuilder()
        if (wrapped.size == 1) {
            append(builder, wrapped.get(0))
        } else {
            builder.append("[")
            append(builder, wrapped)
            builder.append("]")
        }
        return builder.toString()
    }

    fun append(builder: StringBuilder, schemas: List<Schema?>) {
        var hasElement: Boolean = false
        for (item: Schema? in schemas) {
            hasElement = addSeperator(builder, hasElement)
            append(builder, item)
        }
    }

    fun append(builder: StringBuilder, schema: Schema?) {
        builder.append("[")
        var hasAdded: Boolean = false
        if (schema!!.getDescription() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("description:")
            builder.append(schema.getDescription())
        }
        if (schema.getTitle() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("title:")
            builder.append(schema.getTitle())
        }
        if (schema.getRef() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("ref:")
            builder.append(schema.getRef())
        }
        if (schema.getType() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("type:")
            builder.append(schema.getType())
        }
        if (schema.getItems() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("items:")
            append(builder, schema.getItems())
        }
        if (schema.getNullable() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("nullable:")
            builder.append(schema.getNullable())
        }
        if (schema.getAllOf() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("allOf:")
            append(builder, schema.getAllOf())
        }
        if (schema.getAnyOf() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("anyOf:")
            append(builder, schema.getAnyOf())
        }
        if (schema.getOneOf() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("oneOf:")
            append(builder, schema.getOneOf())
        }
        if (schema.getAdditionalPropertiesSchema() != null) {
            hasAdded = addSeperator(builder, hasAdded)
            builder.append("additionalProperties:")
            append(builder, schema.getAdditionalPropertiesSchema())
        }
        builder.append("]")
    }

    fun addSeperator(builder: StringBuilder, hasAppended: Boolean): Boolean {
        if (hasAppended) {
            builder.append(";")
        }
        return true
    }
}
