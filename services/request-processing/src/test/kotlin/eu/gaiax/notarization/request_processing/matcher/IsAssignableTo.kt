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
import org.hamcrest.TypeSafeMatcher

/**
 *
 * @author Neil Crossley
 */
class IsAssignableTo<T> internal constructor(private val given: Class<T>) : TypeSafeMatcher<Class<*>?>() {
    override fun matchesSafely(t: Class<*>?): Boolean {
        return given.isAssignableFrom(t)
    }

    override fun describeTo(description: Description) {
        description.appendText("is assignable to ").appendValue(given)
    }

    companion object {
        fun <T> isAssignableTo(expected: Class<T>): IsAssignableTo<T> {
            return IsAssignableTo(expected)
        }
    }
}
