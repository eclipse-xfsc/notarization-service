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
package eu.gaiax.notarization.request_processing.infrastructure.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.net.URI
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@ConfigMapping(prefix = "gaia-x.extensions")
interface ExtensionServiceConfig {
    fun internalTasks(): Optional<InternalServicesSummary>
    fun tasks(): Map<String, ExternalServiceDescription>
    fun actions(): Map<String, ExternalServiceDescription>

    interface ExternalServiceDescription {
        fun serviceName(): String
        fun names(): Optional<Set<String>>
        fun description(): Optional<String>
        fun location(): Optional<URI>
        @WithDefault(value = "true")
        fun encryptAtRest(): Optional<Boolean>
    }
    interface InternalServicesSummary {
        fun uploadDocuments(): Optional<InternalServiceDescription>
    }

    interface InternalServiceDescription {

        fun serviceNames(): Optional<Set<String>>
        fun names(): Optional<Set<String>>
        @WithDefault(value = "true")
        fun enabled(): Optional<Boolean>
    }
}
