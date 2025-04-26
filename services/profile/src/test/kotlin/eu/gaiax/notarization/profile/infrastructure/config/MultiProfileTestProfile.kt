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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.profile.*
import io.quarkus.test.junit.QuarkusTestProfile
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.lang.JoseException
import java.time.Period


/**
 *
 * @author Neil Crossley
 */
class MultiProfileTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        val overrides = mutableMapOf<String, String>()
        for (index in values.indices) {
            try {
                val profile = values[index]
                val prefix = "gaia-x.profile.config[$index]."
                overrides.putAll(
                    java.util.Map.ofEntries(
                        java.util.Map.entry(prefix + "id", profile.id),
                        java.util.Map.entry(prefix + "name", profile.name),
                        java.util.Map.entry(prefix + "aip", profile.aip.toString()),
                        java.util.Map.entry(prefix + "description", profile.description),
                        java.util.Map.entry(prefix + "notary-roles", profile.notaryRoles.joinToString(",")),
                        java.util.Map.entry(prefix + "valid-for", profile.validFor.toString()),
                        java.util.Map.entry(prefix + "is-revocable", profile.isRevocable.toString()),
                        java.util.Map.entry(prefix + "template", profile.template.toString()),
                        java.util.Map.entry(prefix + "tasks", mapper.writeValueAsString(profile.tasks)),
                        java.util.Map.entry(
                            prefix + "precondition-tasks",
                            mapper.writeValueAsString(profile.preconditionTasks)
                        ),
                        java.util.Map.entry(
                            prefix + "task-descriptions",
                            mapper.writeValueAsString(profile.taskDescriptions)
                        )
                    )
                )
                val issuancePrefix = "gaia-x.profile.issuance[$index]."
                overrides.putAll(
                    mapOf(
                        issuancePrefix + "profile-id" to profile.id
                    )
                )
                if (profile.documentTemplate != null) {
                    overrides[prefix + "document-template"] = profile.template.toString()
                }
                for ((keyIndex, notaryKey) in profile.notaries.withIndex()) {
                    overrides[prefix + "notaries[" + keyIndex + "].algorithm"] = notaryKey.algorithm!!
                    overrides[prefix + "notaries[" + keyIndex + "].jwk"] = notaryKey.key.toJson()
                }
            } catch (ex: JsonProcessingException) {
                throw RuntimeException(ex)
            }
        }
        return overrides
    }

    companion object {
        val mapper = jacksonObjectMapper()
        val values: List<Profile>
        private val publicKey1_1: PublicJsonWebKey
        private val publicKey2_1: PublicJsonWebKey

        init {
            try {
                publicKey1_1 = PublicJsonWebKey.Factory.newPublicJwk(
                    """
                    {"kty":"EC",
                     "crv":"P-256",
                     "x":"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
                     "y":"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM",
                     "use":"enc",
                     "kid":"1"}
                    """
                )
                publicKey2_1 = PublicJsonWebKey.Factory.newPublicJwk(
                    """
                    {"kty":"EC",
                     "d":"gHltlw4V2JKep2v4oRziohrYvU3cvRQ50T3-6WPf0B3d4_2yZ-AGf6sghQVoCPj-",
                     "crv":"P-384",
                     "x":"I9_0yRjMwcRLAhNBwWLPmII2BJK6pIPTqr0LRy0WkoLJiyA846iLUWmLuvM3P8-w",
                     "y":"rPaBxsFyHKPs-h5Pkp6zQ0y5ygdIjvDOc9PTPDqxZnmyBZngFlPNkcl29SSqKzAs"}
                    """
                )
                values = listOf(
                    Profile(
                        "firstProfile",
                        AipVersion.V2_0,
                        CredentialKind.JsonLD,
                        "First name",
                        "Some first description",
                        setOf("role1", "role2", "role3"),
                        "A256GCM",
                        listOf(NotaryAccess("RSA-OAEP-256", publicKey1_1)),
                        Period.ofYears(1),
                        true,
                        mapper.readTree(
                            """
                            {"first object":"other", "first field": 1}
                            """),
                        """
                        { "documents": ["1234"] }
                        """,
                        listOf(TaskDescription("name", TaskType.BROWSER_IDENTIFICATION_TASK, "desc")),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        listOf(),
                        listOf()
                    ),
                    Profile(
                        "second Profile",
                        AipVersion.V2_0,
                        CredentialKind.JsonLD,
                        "Second name",
                        "Some second description",
                        setOf("justAnotherRole"),
                        "A256GCM",
                        listOf(NotaryAccess("ECDH-ES+A256KW", publicKey2_1)),
                        Period.ofYears(1),
                        false,
                        mapper.readTree(
                            """
                            {"second object":"other", "second field": 1}
                            """),
                        null,
                        listOf(TaskDescription("name", TaskType.BROWSER_IDENTIFICATION_TASK, "desc")),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        listOf(),
                        listOf()
                    ),
                    Profile(
                        "third Profile",
                        AipVersion.V1_0,
                        CredentialKind.AnonCred,
                        "Third name",
                        "Some third description",
                        setOf("admin"),
                        "A256GCM",
                        java.util.List.of(NotaryAccess("ECDH-ES+A256KW", publicKey2_1)),
                        Period.ofDays(6),
                        true,
                        mapper.readTree(
                            """
                            {"third object":"other", "third field": 1}
                            """),
                        """
                        { "documents": ["654", 1234"] }
                        """,
                        listOf(),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        listOf(),
                        listOf()
                    ),
                    Profile(
                        "fourth-Profile",
                        AipVersion.V2_0,
                        CredentialKind.JsonLD,
                        "Fourth name",
                        "Some fourth description",
                        setOf("asRole", "myRole", "givenRole"),
                        "A256GCM",
                        listOf(NotaryAccess("ECDH-ES+A256KW", publicKey2_1)),
                        Period.ofYears(1),
                        false,
                        mapper.readTree(
                            """
                            {"fourth object": "other", "fourth field": 1}
                            """
                        ),
                        null,
                        listOf(),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        ProfileTaskTree(),
                        listOf(),
                        listOf()
                    )
                )
            } catch (ex: JoseException) {
                throw RuntimeException(ex)
            } catch (ex: JsonProcessingException) {
                throw RuntimeException(ex)
            }
        }
    }
}
