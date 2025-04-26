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
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.profile.*
import io.quarkus.test.junit.QuarkusTestProfile
import mu.KotlinLogging
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.lang.JoseException
import java.time.Period

private val logger = KotlinLogging.logger {}
/**
 *
 * @author Neil Crossley
 */
class ZeroNotariesProfileTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        return try {
            val prefix = "gaia-x.profile.config[0]."
            java.util.Map.ofEntries(
                java.util.Map.entry(prefix + "id", singleValue.id),
                java.util.Map.entry(prefix + "aip", singleValue.aip.toString()),
                java.util.Map.entry(prefix + "name", singleValue.name),
                java.util.Map.entry(prefix + "description", singleValue.description),
                java.util.Map.entry(prefix + "valid-for", singleValue.validFor.toString()),
                java.util.Map.entry(prefix + "template", singleValue.template.toString()),
                java.util.Map.entry(
                    prefix + "tasks", mapper.writeValueAsString(
                        singleValue.tasks
                    )
                ),
                java.util.Map.entry(
                    prefix + "precondition-tasks", mapper.writeValueAsString(
                        singleValue.preconditionTasks
                    )
                ),
                java.util.Map.entry(
                    prefix + "task-descriptions", mapper.writeValueAsString(
                        singleValue.taskDescriptions
                    )
                )
            )
        } catch (ex: JsonProcessingException) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        val mapper = jacksonObjectMapper()
        val singleValue: Profile
        private val publicKey1: PublicJsonWebKey

        init {
            try {
                publicKey1 = PublicJsonWebKey.Factory.newPublicJwk(
                    """
                    {"kty":"EC",
                     "crv":"P-256",
                     "x":"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
                     "y":"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM",
                     "use":"enc",
                     "kid":"1"}
                    """
                )
            } catch (ex: JoseException) {
                throw RuntimeException(ex)
            }
            val template: JsonNode
            try {
                template =
                    mapper.readTree(
                        """
                        {"something":"other"}
                        """
                    )
            } catch (ex: JsonProcessingException) {
                throw RuntimeException(ex)
            }
            singleValue = Profile(
                "someId",
                AipVersion.V2_0,
                CredentialKind.JsonLD,
                "some Name",
                "some description",
                setOf("anyRole"),
                "A256GCM",
                listOf<NotaryAccess>(),
                Period.ofYears(1),
                true,
                template,
                null,
                listOf<TaskDescription>(
                    TaskDescription("name", TaskType.BROWSER_IDENTIFICATION_TASK, "desc"),
                    TaskDescription("name2", TaskType.BROWSER_IDENTIFICATION_TASK, "desc")
                ),
                ProfileTaskTree(),
                ProfileTaskTree(),
                ProfileTaskTree(),
                listOf(),
                listOf()
            )
        }
    }
}
