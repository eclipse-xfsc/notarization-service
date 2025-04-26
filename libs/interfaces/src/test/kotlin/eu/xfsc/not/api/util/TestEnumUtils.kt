package eu.xfsc.not.api.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestEnumUtils {
    @Test
    fun testEnumSerde() {
        val om = jacksonObjectMapper()
        for (e in TestEnum.entries) {
            val s1 = om.writeValueAsString(e)
            assertEquals("\"${e.value}\"", s1)
            val eRead: TestEnum = om.readValue(s1)
            assertEquals(e, eRead)
        }
    }

    @Test
    fun testEnumFromString() {
        val a: TestEnum? = fromString("value a")
        assertEquals(TestEnum.A, a)
        val b: TestEnum? = fromString("value b")
        assertEquals(TestEnum.B, b)
    }
}


enum class TestEnum (override val value: String) : JsonValueEnum {
    A("value a"), B("value b");
}
