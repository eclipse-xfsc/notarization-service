/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.gaiax.notarization.profile.infrastructure.rest.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import io.smallrye.openapi.api.util.MergeUtil
import jakarta.json.JsonPointer
import jakarta.json.JsonValue
import mu.KotlinLogging
import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.OpenAPI
import org.eclipse.microprofile.openapi.models.Operation
import org.eclipse.microprofile.openapi.models.media.Schema
import org.eclipse.microprofile.openapi.models.parameters.Parameter
import org.eclipse.microprofile.openapi.models.parameters.RequestBody
import org.eclipse.microprofile.openapi.models.security.SecurityScheme
import org.jboss.resteasy.reactive.multipart.FileUpload
import org.jose4j.jwk.PublicJsonWebKey
import java.security.Key
import java.security.Principal
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.stream.Collectors
import java.util.stream.Stream

private val logger = KotlinLogging.logger { }

/**
 *
 * @author Neil Crossley
 */
class OpenApiCorrection() : OASFilter {

    companion object {
        const val CDI_TAG = "CDI Wrapper"
    }
    private val unwantedClasses = setOf(
        JsonNode::class.java,
        JsonPointer::class.java,
        JsonValue::class.java,
        JsonNodeType::class.java,
        JsonValue.ValueType::class.java,
        FileUpload::class.java,
        ProfileTaskTree::class.java,
        PublicJsonWebKey::class.java,
        PrivateKey::class.java,
        X509Certificate::class.java,
        PublicKey::class.java,
        Key::class.java,
        Principal::class.java
    )
    private val unwantedNames = setOf(
        "JsonValue1",
        "ValueType1"
    )
    private val unwantedComponents = Stream.concat(unwantedClasses.stream()
        .map { clazz: Class<*> -> clazz.simpleName }, unwantedNames.stream()
    )
        .collect(Collectors.toSet())
    private val unwantedReferences = unwantedComponents.stream()
        .map { component: String -> "#/components/schemas/$component" }
        .collect(Collectors.toSet())
    private val unwantedTags = setOf(
        CDI_TAG
    )
    private val foundReferences: MutableList<Schema> = ArrayList()

    override fun filterOpenAPI(openAPI: OpenAPI) {
        val componentSchemas: MutableMap<String, Schema> = HashMap<String, Schema>(openAPI.components.schemas)
        for (unwantedComponent: String in unwantedComponents) {
            val removed = componentSchemas.remove(unwantedComponent)
            if (removed != null) {
                logger.debug { "Removed component $unwantedComponent" }
            }
        }
        openAPI.components.schemas = componentSchemas
        val removablePaths = HashSet<String>()
        for ((path, pathItem) in openAPI.paths.pathItems.entries) {
            for ((key, value) in pathItem.operations.entries) {
                if (!isWanted(value)) {
                    logger.debug { "Removing operation [$path:$key]" }
                    pathItem.setOperation(key, null)
                }
            }
            if (pathItem.operations.isEmpty()) {
                logger.debug { "Removing path [$path]" }
                removablePaths.add(path)
            }
        }
        if (removablePaths.isNotEmpty()) {
            val replacementPathItems =
                openAPI.paths.pathItems.toMutableMap()
            replacementPathItems.keys.removeAll(removablePaths)
            openAPI.paths.pathItems = replacementPathItems
        }
    }

    private fun isWanted(operation: Operation): Boolean {
        val currentTags = operation.tags
        if (currentTags == null || currentTags.isEmpty()) {
            return true
        }
        for (currentTag: String in currentTags) {
            if (unwantedTags.contains(currentTag)) {
                logger.debug {
                    "Removing unwanted operation [${operation.operationId},${operation.summary}] with tag $currentTag"
                }
                return false
            }
        }
        return true
    }

    override fun filterSecurityScheme(securityScheme: SecurityScheme): SecurityScheme? {
        val securityType = securityScheme.type
        if (securityType == SecurityScheme.Type.OPENIDCONNECT) {
            val connectUrl = securityScheme.openIdConnectUrl
            if (connectUrl == null) {
                securityScheme.openIdConnectUrl = "http://example.localhost/.well-known/openid-configuration"
            }
        }
        return securityScheme
    }

    override fun filterParameter(parameter: Parameter): Parameter {
        val schema = parameter.schema
        val result = sanitize(schema)
        if (result.changed) {
            logger.debug {
                "Parameter ''${parameter.name}'' had schema ${SchemaStringWrapper(schema)}, is now ${
                    SchemaStringWrapper(
                        result.value
                    )
                }"
            }
            parameter.schema = result.value
        }
        return parameter
    }

    override fun filterRequestBody(requestBody: RequestBody): RequestBody {
        val content = requestBody.content
        for ((key, mediaType) in content.mediaTypes) {
            val originalSchema = mediaType.schema
            val result = sanitize(originalSchema)
            if (result.changed) {
                logger.debug {
                    "Mediatype ''$key'' was ${SchemaStringWrapper(originalSchema)}, is now ${SchemaStringWrapper(result.value)}"
                }
                mediaType.schema = result.value
            }
        }
        return requestBody
    }

    override fun filterSchema(schema: Schema): Schema {
        val props = schema.properties
        if (props != null) {
            val changes = HashMap<String, Schema?>()
            for (entry in props.entries) {
                val key = entry.key
                val value = entry.value
                val result = sanitize(value)
                if (result.changed) {
                    logger.debug {
                        "Property ''$key'' was ${SchemaStringWrapper(value)}, is now ${SchemaStringWrapper(result.value)}"
                    }
                    changes[key] = result.value
                }
            }
            if (changes.isNotEmpty()) {
                for (entry: Map.Entry<String, Schema> in props.entries) {
                    val key = entry.key
                    val value = entry.value
                    changes.putIfAbsent(key, value)
                }
                schema.properties = changes
            }
        }
        return schema
    }

    private fun sanitize(schema: Schema?): ChangeStatus<Schema?> {
        if (schema == null) {
            return ChangeStatus.unchanged(null)
        }
        try {
            val withoutUnwanted = removeUnwantedReferences(schema)
            val simplifiedAllOf = compactAllOf(withoutUnwanted.value)
            val compacted = simplifySingleAllOfRef(simplifiedAllOf.value)
            val simplifiedType = simplifyType(compacted.value)
            storeReferences(simplifiedType.value)
            return ChangeStatus(
                compacted.value,
                simplifiedAllOf.changed || withoutUnwanted.changed || compacted.changed || simplifiedType.changed
            )
        } catch (t: Throwable) {
            logger.error(t) { "An error occurred while processing changes to the OpenAPI specification" }
            throw t
        }
    }

    private fun compactAllOf(propertySchema: Schema): ChangeStatus<Schema> {
        val allOf = propertySchema.allOf
        if (allOf != null && allOf.isNotEmpty()) {
            var mergedResult: Schema = MergeUtil.mergeObjects(OASFactory.createSchema(), propertySchema)
            val resultAllOf: MutableList<Schema> = ArrayList(allOf.size)
            for (schema: Schema? in allOf) {
                if (schema != null) {
                    if (schema.type == null && schema.ref == null) {
                        mergedResult = MergeUtil.mergeObjects(mergedResult, schema)
                    } else {
                        resultAllOf.add(schema)
                    }
                }
            }
            if (resultAllOf.isEmpty()) {
                mergedResult.allOf = null
            } else if (resultAllOf.size == 1) {
                mergedResult.allOf = null
                mergedResult = MergeUtil.mergeObjects(mergedResult, resultAllOf[0])
            } else {
                mergedResult.allOf = resultAllOf
            }
            return ChangeStatus.changed(mergedResult)
        } else {
            return ChangeStatus.unchanged(propertySchema)
        }
    }

    private fun removeUnwantedReferences(propertySchema: Schema): ChangeStatus<Schema> {
        val allOf = propertySchema.allOf
        if (allOf != null && allOf.isNotEmpty()) {
            val result = filterWanted(allOf)
            if (result.hasUnwanted) {
                val mergedResult: Schema = MergeUtil.mergeObjects(OASFactory.createSchema(), propertySchema)
                mergedResult.allOf = result.wanted
                val compactedResult = compactAllOf(mergedResult)
                if (compactedResult.changed) {
                    val compacted = compactedResult.value
                    if (compacted.allOf == null || compacted.allOf.isEmpty()) {
                        compacted.allOf = null
                        if (compacted.type == null) {
                            compacted.type = Schema.SchemaType.OBJECT
                        }
                    }
                    return ChangeStatus.changed(compacted)
                } else {
                    return ChangeStatus.changed(mergedResult)
                }
            }
        }
        val anyOf = propertySchema.anyOf
        if (anyOf != null && anyOf.size >= 2) {
            val result = filterWanted(anyOf)
            if (result.hasUnwanted) {
                if (result.wanted.size > 1) {
                    propertySchema.anyOf = result.wanted
                    return ChangeStatus.changed(propertySchema)
                } else {
                    propertySchema.anyOf = null
                    return ChangeStatus.changed(result.wanted[0])
                }
            }
        }
        val ref = propertySchema.ref
        if (ref != null && unwantedReferences.contains(ref)) {
            logger.debug { "Removing ref: $ref" }
            propertySchema.ref = null
            propertySchema.type = Schema.SchemaType.OBJECT
            return ChangeStatus.changed(propertySchema)
        }
        return ChangeStatus.unchanged(propertySchema)
    }

    private fun simplifySingleAllOfRef(propertySchema: Schema): ChangeStatus<Schema> {
        val allOf = propertySchema.allOf
        if (allOf != null) {
            var refTypeCount = 0
            if (allOf.size >= 2) {
                for (currentSchema: Schema in allOf) {
                    if (currentSchema.ref != null) {
                        refTypeCount += 1
                        continue
                    }
                    if (currentSchema.type != null) {
                        refTypeCount += 1
                        continue
                    }
                }
            } else if (allOf.size == 0) {
                refTypeCount = 1
            }
            if (refTypeCount == 1) {
                var mergedResult: Schema = MergeUtil.mergeObjects<Schema>(OASFactory.createSchema(), propertySchema)
                mergedResult.allOf = null
                for (currentSchema: Schema in allOf) {
                    mergedResult = MergeUtil.mergeObjects<Schema>(mergedResult, currentSchema)
                }
                return ChangeStatus.changed(mergedResult)
            }
        }
        return ChangeStatus.unchanged(propertySchema)
    }

    private fun simplifyType(propertySchema: Schema): ChangeStatus<Schema> {
        val currentType = propertySchema.type
        if (currentType != null) {
            if (currentType == Schema.SchemaType.ARRAY) {
                val items = propertySchema.items
                if (items == null) {
                    propertySchema.type = null
                    return ChangeStatus.changed(propertySchema)
                }
            }
        }
        return ChangeStatus.unchanged(propertySchema)
    }

    private fun filterWanted(schemas: List<Schema>): FilterResults {
        val wanted: MutableList<Schema> =
            ArrayList(schemas.size)
        var unwanted = 0
        for (currentSchema: Schema in schemas) {
            val currentRef = currentSchema.ref
            if (currentRef != null && unwantedReferences.contains(currentRef)) {
                unwanted += 1
            } else {
                wanted.add(currentSchema)
            }
        }
        return FilterResults(unwanted > 0, wanted)
    }

    private fun storeReferences(result: Schema) {
        if (result.ref != null) {
            foundReferences.add(result)
        }
        storeReferences(result.oneOf)
        storeReferences(result.allOf)
        storeReferences(result.anyOf)
        val items = result.items
        if (items != null) {
            if (items.ref != null) {
                foundReferences.add(items)
            }
        }
    }

    private fun storeReferences(schemas: List<Schema>?) {
        if (schemas == null) {
            return
        }
        for (schema: Schema in schemas) {
            if (schema.ref != null) {
                foundReferences.add(schema)
            }
        }
    }

    private class FilterResults(val hasUnwanted: Boolean, val wanted: List<Schema>)
    private class ChangeStatus<T>(val value: T, val changed: Boolean) {
        companion object {
            fun <T> changed(value: T): ChangeStatus<T> {
                return ChangeStatus(value, true)
            }

            fun <T> unchanged(value: T): ChangeStatus<T> {
                return ChangeStatus(value, false)
            }
        }
    }

}
