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
 ***************************************************************************/

package eu.gaiax.notarization.profile.infrastructure.config;

import eu.gaiax.notarization.api.profile.*
import java.util.Objects;

/**
 *
 * @author Neil Crossley
 */
class ProfileWrappingEquality(val profile: Profile) {

    companion object {

        fun comparable(profile: Profile): ProfileWrappingEquality {
            return ProfileWrappingEquality(profile);
        }

        fun comparable(profile: List<Profile>): List<ProfileWrappingEquality> {
            return profile.map(ProfileWrappingEquality::comparable);
        }
    }

    @Override
    override fun equals(other: Any?): Boolean {
        if (other is ProfileWrappingEquality) {
            return this.equalsProfile(other.profile);
        }
        if (other !is Profile) {
            return false;
        }

        return equalsProfile(other);
    }

    fun equalsProfile(other: Profile): Boolean {
        return Objects.equals(profile.id, other.id) &&
                Objects.equals(profile.name, other.name) &&
                Objects.equals(profile.description, other.description) &&
                Objects.equals(profile.notaryRoles, other.notaryRoles) &&
                Objects.equals(profile.isRevocable, other.isRevocable) &&
                Objects.equals(profile.actionDescriptions, other.actionDescriptions) &&
                Objects.equals(profile.template, other.template) &&
                Objects.equals(profile.validFor, other.validFor) &&
                equals(profile.notaries, other.notaries);
    }

    private fun equals(given: List<NotaryAccess> , other: List<NotaryAccess>): Boolean {
        return itemsMatch(given, other) && itemsMatch(other, given);
    }

    private fun itemsMatch(left: List<NotaryAccess>, right: List<NotaryAccess>): Boolean {
        for (outerAccess in left) {
            var hasMatch: Boolean = false
            val outerKey = outerAccess.key.toJson();
            for (innerAccess in right) {
                if (Objects.equals(outerAccess.algorithm, innerAccess.algorithm) &&
                        Objects.equals(outerKey, innerAccess.key.toJson())) {
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                return false;
            }
        }

        return true;
    }

    @Override
    override fun hashCode(): Int {
        val prime: Int = 31;
        var result: Int = 1;
        result = prime * result + profile.id.hashCode()
        result = prime * result + profile.description.hashCode()
        result = prime * result + profile.name.hashCode()
        result = prime * result + profile.template.hashCode()
        return result;
    }
}
