package eu.xfsc.not.api.util

import com.fasterxml.jackson.annotation.JsonValue


/**
 * Interface for enums that are represented by a constant.
 * By applying this interface, the enum can be used in Jackson models and automatically de-/serializes from/to the string constant in [value].
 * When using this interface, the enum can be deserialized with the [fromString] function.
 */
interface JsonValueEnum {
    /**
     * Internal property to store the string constant.
     * This is needed in order to preserve the `JsonValue` annotation on the `jsonValue` property.
     */
    val value: String

    /**
     * Property to define how the enum is serialized.
     * Don't override if you want to automatically use the `value` property as the serialized value.
     */
    @get:JsonValue
    val jsonValue: String get() { return value }
}

/**
 * Deserializes a string to an enum value, given the enum implements [JsonValueEnum].
 */
inline fun <reified T : JsonValueEnum> fromString(s: String): T? {
    T::class.java.enumConstants.forEach {
        if (it.value == s) {
            return it
        }
    }
    return null
}
