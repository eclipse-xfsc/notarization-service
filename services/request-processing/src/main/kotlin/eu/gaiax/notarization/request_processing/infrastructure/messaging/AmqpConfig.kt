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
package eu.gaiax.notarization.request_processing.infrastructure.messaging

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.util.*

@ConfigMapping(prefix = "notarization.amqp")
interface AmqpConfig {

    fun cloudEventTypePrefix(): String

    fun caPath(): Optional<String>
    fun tls(): Optional<Tls>

    @WithDefault("false")
    fun ssl(): Boolean
    interface Tls {
        fun certPath(): String
        fun keyPath(): String
    }
}
