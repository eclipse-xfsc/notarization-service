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
package eu.gaiax.notarization.profile.infrastructure.resource.nossi

import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.profile.infrastructure.MockOauth2ServerResource
import eu.gaiax.notarization.profile.infrastructure.rest.client.*
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*


/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@TestProfile(NoSsiIssuanceProfile::class)
@Disabled
@QuarkusTestResource(MockOauth2ServerResource::class)
open class NoSsiClientTest {

    @Inject
    lateinit var sut: Set<ApiVersion>
    @Inject
    lateinit var v1: Optional<SsiIssuanceV1HttpClient>
    @Inject
    lateinit var v2: Optional<SsiIssuanceV2HttpClient>

    @Test
    fun hasCorrectApiVersions() {
        assertThat(sut, equalTo(setOf()))
    }
    @Test
    fun hasCorrectV1Client() {
        assertThat(v1.isEmpty, equalTo(true))
    }
    @Test
    fun hasCorrectV2Client() {
        assertThat(v2.isEmpty, equalTo(true))
    }
}
