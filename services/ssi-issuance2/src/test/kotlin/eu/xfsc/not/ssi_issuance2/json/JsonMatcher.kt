package eu.xfsc.not.ssi_issuance2.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.Is.`is`

class WithJsonField<T: JsonNode>(
    private val fieldName: String,
    private val valueMatcher: Matcher<T>,
) : TypeSafeDiagnosingMatcher<T>(JsonNode::class.java) {

    override fun matchesSafely(item: T, mismatchDescription: Description): Boolean {
        val jsonNode = item[fieldName]
        if (jsonNode == null) {
            mismatchDescription.appendText("no field ").appendValue(fieldName).appendText(" found")
            return false
        }
        if (!valueMatcher.matches(jsonNode)) {
            mismatchDescription.appendText("value of field ").appendValue(fieldName).appendText(" did not match ")
                .appendValue(valueMatcher).appendText(", ")
            valueMatcher.describeMismatch(jsonNode, mismatchDescription)
            return false
        }
        return true
    }

    override fun describeTo(description: Description) {
        description.appendText("withJsonField(")
            .appendValue(fieldName)
            .appendText(")");
        if (this.valueMatcher is TypeSafeDiagnosingMatcher) {

        }
    }

    companion object {

        fun <T : JsonNode> withJsonField(property: String, value: String?): WithJsonField<T> {
            return WithJsonField(property, IsJsonText.isJsonText(value))
        }

        fun <T : JsonNode> withJsonField(property: String, vararg valueMatchers: Matcher<T>): WithJsonField<T> {
            return WithJsonField(property, allOf(*valueMatchers))
        }
        fun <T: JsonNode> matchesJsonText(property: String, matcher: Matcher<String?>): WithJsonField<T> {
            return WithJsonField(property, MatchesJsonText.matchesJsonText(matcher))
        }
    }
}

class IsJsonText<T: JsonNode>(private val targetText: String?) : TypeSafeMatcher<T>(JsonNode::class.java) {

    override fun describeTo(description: Description) {
        description.appendText("isJsonText(")
            .appendValue(targetText)
            .appendText(")")
    }

    override fun matchesSafely(item: T): Boolean {
        if (item is NullNode && targetText == null) {
            return true
        }
        val text = item.asText()
        return `is`(targetText).matches(text)
    }

    override fun describeMismatchSafely(item: T, mismatchDescription: Description) {
        val text = item.asText()
        `is`(targetText).describeMismatch(text, mismatchDescription)
    }

    companion object {
        fun <T: JsonNode> isJsonText(targetText: String?): IsJsonText<T> {
            return IsJsonText(targetText)
        }
    }
}

class MatchesJsonText<T: JsonNode>(private val matcher: Matcher<String?>) : TypeSafeMatcher<T>(JsonNode::class.java) {

    override fun describeTo(description: Description) {
        description.appendText("isJsonText(")
        matcher.describeTo(description)
        description.appendText(")")
    }

    override fun matchesSafely(item: T): Boolean {
        val text = item.asText()
        return matcher.matches(text)
    }

    override fun describeMismatchSafely(item: T, mismatchDescription: Description) {
        val text = item.asText()
        matcher.describeMismatch(text, mismatchDescription)
    }

    companion object {
        fun <T: JsonNode> matchesJsonText(matcher: Matcher<String?>): MatchesJsonText<T> {
            return MatchesJsonText(matcher)
        }
    }
}

class IsJsonArray<T: JsonNode>(private val matchers: MutableList<Matcher<in T>>) : TypeSafeMatcher<T>(JsonNode::class.java) {
    constructor(vararg values: Any) : this(mutableListOf()) {
        for (matcher in values) {
            if (matcher is Matcher<*>) {
                matchers.add(matcher as Matcher<in T>)
            } else if (matcher is String) {
                matchers.add(IsJsonText.isJsonText(matcher))
            } else {
                throw IllegalArgumentException("Cannot create matcher for type ${matcher.javaClass} for the value $matcher")
            }
        }
    }

    override fun describeTo(description: Description) {
        description.appendText("isJsonArray(").appendValue(matchers).appendText(")")
    }

    override fun describeMismatchSafely(item: T, mismatchDescription: Description) {
        if (!item.isArray) {
            mismatchDescription.appendValue(item).appendText(" is not an array")
            return
        }
        val list = asList(item)
        if (matchers.isEmpty()) {
            Matchers.emptyIterable<T>().describeMismatch(list, mismatchDescription)
        } else {
            Matchers.contains(matchers).describeMismatch(list, mismatchDescription)
        }

    }

    override fun matchesSafely(item: T): Boolean {
        if (!item.isArray) {
            return false
        }
        val list = asList(item)
        if (matchers.isEmpty()) {
            return Matchers.emptyIterable<T>().matches(list)
        }
        return Matchers.contains(matchers).matches(item)
    }

    private fun asList(value: T): List<JsonNode> {
        val results = mutableListOf<JsonNode>()
        for (item in value) {
            results.add(item)
        }
        return results
    }

    companion object {
        fun <T: JsonNode> isJsonArray(vararg array: Any): IsJsonArray<T> {
            return IsJsonArray(*array)
        }
    }
}
