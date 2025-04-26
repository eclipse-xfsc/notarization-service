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
package eu.gaiax.notarization.profile.infrastructure.config

import eu.gaiax.notarization.api.profile.*
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 *
 * @author Neil Crossley
 */
class ProfileMatcher(val expected: Profile) : TypeSafeMatcher<Profile>() {
    override fun matchesSafely(item: Profile): Boolean {
        return (item.id == expected.id && item.name == expected.name && item.description == expected.description && item.template == expected.template && item.validFor == expected.validFor
                && equals(item.notaries, expected.notaries))
    }

    private fun equals(given: List<NotaryAccess>, other: List<NotaryAccess>): Boolean {
        return itemsMatch(given, other) && itemsMatch(other, given)
    }

    private fun itemsMatch(left: List<NotaryAccess>, right: List<NotaryAccess>): Boolean {
        for (outerAccess in left) {
            var hasMatch = false
            val outerKey = outerAccess.key.toJson()
            for (innerAccess in right) {
                if (outerAccess.algorithm == innerAccess.algorithm && outerKey == innerAccess.key.toJson()) {
                    hasMatch = true
                    break
                }
            }
            if (!hasMatch) {
                return false
            }
        }
        return true
    }

    override fun describeTo(description: Description) {
        description.appendValue(expected)
    }

    companion object {
        @JvmStatic
        fun equalsProfile(profile: Profile): ProfileMatcher {
            return ProfileMatcher(profile)
        }
    }
}
