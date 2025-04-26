package eu.xfsc.not.testutil

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*

val om = jacksonObjectMapper()

fun assertEqualJsonTree(expected: String, actual: String, ignoreExtraFields: Boolean = false) {
    val expectedTree = om.readTree(expected)
    val actualTree = om.readTree(actual)
    assertEqualJsonTreeNode(expectedTree, actualTree, ignoreExtraFields)
}

fun assertEqualJsonTreeNode(expected: JsonNode, actual: JsonNode, ignoreExtraFields: Boolean = false) {
    // compare the nodes
    assertEquals(expected.nodeType, actual.nodeType, "Node type mismatch")

    if (expected.isObject) {
        if (!ignoreExtraFields) {
            for (actField in actual.fieldNames()) {
                assertTrue(expected.has(actField), "Actual field missing in expected: $actField")
            }
        }
        for (expectField in expected.fieldNames()) {
            assertTrue(actual.has(expectField), "Expected field missing: $expectField")
        }
        // recurse per field
        for (field in expected.fieldNames()) {
            assertEqualJsonTreeNode(expected.get(field), actual.get(field), ignoreExtraFields)
        }
    } else if (expected.isArray) {
        assertEquals(expected.size(), actual.size(), "Array size mismatch")
        for (i in 0 until expected.size()) {
            assertEqualJsonTreeNode(expected.get(i), actual.get(i), ignoreExtraFields)
        }
    } else if (expected.isValueNode) {
        assertEquals(expected.asText(), actual.asText(), "Value mismatch")
    } else {
        fail("Unknown node type (${expected.nodeType})")
    }
}
