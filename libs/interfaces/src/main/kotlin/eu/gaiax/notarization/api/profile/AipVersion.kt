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
package eu.gaiax.notarization.api.profile

import com.fasterxml.jackson.annotation.JsonValue
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
@Schema(
    description = "The Aries Interop Profile (AIP) version.",
    enumeration = [AipVersion.Name.V1_0, AipVersion.Name.V2_0],
    deprecated = true
)
@Deprecated(message = "Use CredentialKind instead", replaceWith = ReplaceWith("CredentialKind"))
enum class AipVersion(private val value: String) {
    V1_0(Name.V1_0), V2_0(Name.V2_0);

    fun asCredentialKind(): CredentialKind {
        return if (this == V1_0) {
            CredentialKind.AnonCred
        } else if (this == V2_0) {
            CredentialKind.JsonLD
        } else {
            throw IllegalArgumentException("Unsupported Aip version: $this")
        }
    }
    @JsonValue
    override fun toString(): String {
        return value
    }
    object Name {
        const val V1_0 = "1.0"
        const val V2_0 = "2.0"
    }

    companion object {
        fun fromString(s: String): AipVersion {
            if (s == Name.V1_0) {
                return V1_0
            } else if (s == Name.V2_0) {
                return V2_0
            }
            run { throw IllegalArgumentException("The following value is not a valid AIP version specifier: $s") }
        }
    }
}
