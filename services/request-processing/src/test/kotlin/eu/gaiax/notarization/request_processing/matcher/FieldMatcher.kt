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
package eu.gaiax.notarization.request_processing.matcher

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.lang.reflect.Field

/**
 *
 * @author Neil Crossley
 */
class FieldMatcher<T, V>(private val fieldName: String, private val matcher: Matcher<V>) : TypeSafeMatcher<T>() {
    override fun matchesSafely(item: T): Boolean {
        if (item == null) {
            return false
        }
        val javaClass = item!!::class.java
        val foundField: Field = try {
            javaClass.getField(fieldName)
        } catch (ex: NoSuchFieldException) {
            try {
                javaClass.getDeclaredField(fieldName)
            } catch (ex1: NoSuchFieldException) {
                return false
            } catch (ex1: SecurityException) {
                return false
            }
        } catch (ex: SecurityException) {
            return false
        }
        foundField.isAccessible = true
        val foundValue: Any = try {
            foundField[item]
        } catch (ex: IllegalArgumentException) {
            return false
        } catch (ex: IllegalAccessException) {
            return false
        } catch (ex: ClassCastException) {
            return false
        }
        return matcher.matches(foundValue)
    }

    override fun describeMismatchSafely(item: T, mismatchDescription: Description) {
        val javaClass = item!!::class.java
        val foundField: Field = try {
            javaClass.getField(fieldName)
        } catch (ex: NoSuchFieldException) {
            try {
                javaClass.getDeclaredField(fieldName)
            } catch (ex1: NoSuchFieldException) {
                mismatchDescription.appendText("was missing field named ")
                    .appendValue(fieldName)
                    .appendText(" on ")
                    .appendValue(item)
                return
            } catch (ex1: SecurityException) {
                mismatchDescription.appendText("could not access field name ")
                    .appendValue(fieldName)
                    .appendText(" on ")
                    .appendValue(item)
                return
            }
        } catch (ex: SecurityException) {
            mismatchDescription.appendText("could not access field name ")
                .appendValue(fieldName)
                .appendText(" on ")
                .appendValue(item)
            return
        }
        foundField.isAccessible = true
        val foundValue: Any = try {
            foundField[item]
        } catch (ex: IllegalArgumentException) {
            mismatchDescription.appendText("could not access field named ")
                .appendValue(fieldName)
                .appendText(" on ")
                .appendValue(item)
            return
        } catch (ex: IllegalAccessException) {
            mismatchDescription.appendText("could not access value of field named ")
                .appendValue(fieldName)
                .appendText(" on ")
                .appendValue(item)
            return
        }
        matcher.describeMismatch(foundValue, mismatchDescription)
    }

    override fun describeTo(d: Description) {
        d.appendText("a field ")
            .appendValue(fieldName)
            .appendText(" [")
            .appendDescriptionOf(matcher)
            .appendValue("]")
    }

    companion object {
        fun <T, V> hasField(name: String, matcher: Matcher<V>): FieldMatcher<T, V> {
            return FieldMatcher(name, matcher)
        }
    }
}
