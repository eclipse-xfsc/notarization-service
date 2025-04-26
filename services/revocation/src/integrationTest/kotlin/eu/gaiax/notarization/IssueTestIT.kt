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

package eu.gaiax.notarization

import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile


@QuarkusIntegrationTest
@TestProfile(IntegrationTestProfile::class)
class IssueTestIT : IssueTest()

class IntegrationTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): MutableMap<String, String> {
        return mutableMapOf(
            "revocation.base-url" to "https://example.com/revocation",
            "quarkus.flyway.migrate-at-start" to "true",
            "quarkus.flyway.clean-at-start" to "true",
            "%prod.quarkus.log.level" to "INFO",
        )
    }
}
