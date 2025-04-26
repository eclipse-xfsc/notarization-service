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

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithConverter
import io.smallrye.config.WithDefault
import org.jose4j.jwk.PublicJsonWebKey
import java.time.Period
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@ConfigMapping(prefix = "gaia-x.profile")
interface ProfileSourceConfig {
    fun defaultAlgorithms(): Map<String, String>
    fun config(): List<ProfileConfig>
    interface ProfileConfig {
        fun id(): String
        @WithConverter(AipVersionConverter::class)
        fun aip(): Optional<AipVersion>
        @WithConverter(CredentialKindConverter::class)
        fun kind(): Optional<CredentialKind>
        fun name(): String
        fun description(): String
        fun notaryRoles(): Optional<Set<String>>
        @WithDefault(value = "A256GCM")
        fun encryption(): String
        fun notaries(): List<NotaryAccessConfig>
        fun validFor(): Optional<Period>
        @WithConverter(ObjectNodeConverter::class)
        fun template(): ObjectNode
        fun documentTemplate(): Optional<String?>
        @WithConverter(JsonArrayConverter::class)
        fun taskDescriptions(): ArrayNode

        @get:WithDefault(value = "true")
        val isRevocable: Boolean
        @WithConverter(ProfileTaskTreeConverter::class)
        fun tasks(): ProfileTaskTree
        @WithConverter(ProfileTaskTreeConverter::class)
        fun preconditionTasks(): ProfileTaskTree
        fun postIssuanceActions(): Optional<List<String>>
        @WithConverter(ProfileTaskTreeConverter::class)
        fun preIssuanceActions(): Optional<ProfileTaskTree>
        @WithConverter(JsonArrayConverter::class)
        fun actionDescriptions(): ArrayNode
        fun autoKeys(): Optional<Boolean>
    }

    interface NotaryAccessConfig {
        fun algorithm(): Optional<String>
        @WithConverter(JsonWebKeyConverter::class)
        fun jwk(): PublicJsonWebKey
    }

    fun issuance(): List<KeyConfiguration>

    interface KeyConfiguration {

        fun profileId(): String
        fun version(): Optional<ApiVersion>
        @WithConverter(ObjectNodeConverter::class)
        fun spec(): Optional<ObjectNode>
    }
}
