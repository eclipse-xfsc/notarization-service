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
package eu.gaiax.notarization

import eu.gaiax.notarization.request_processing.infrastructure.messaging.FrontEndMessageService
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.smallrye.reactive.messaging.memory.InMemoryConnector

/**
 *
 * @author Neil Crossley
 */
class RabbitMqTestResourceLifecycleManager : QuarkusTestResourceLifecycleManager {
    override fun start(): Map<String, String> {
        val env: MutableMap<String, String> = HashMap()
        val props1 =
            InMemoryConnector.switchOutgoingChannelsToInMemory(FrontEndMessageService.outgoingOperatorRequestChanged)
        val props2 =
            InMemoryConnector.switchOutgoingChannelsToInMemory(FrontEndMessageService.outgoingRequestorRequestChanged)
        env.putAll(props1)
        env.putAll(props2)
        return env
    }

    override fun stop() {
        InMemoryConnector.clear()
    }
}
