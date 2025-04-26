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
package eu.gaiax.notarization.profile.infrastructure.resource.ssiv2

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.profile.domain.entity.PersistantProfile
import eu.gaiax.notarization.profile.domain.entity.ProfileDid
import eu.gaiax.notarization.profile.infrastructure.MockOauth2ServerResource
import eu.gaiax.notarization.profile.infrastructure.config.ConfigBackedProfileService
import eu.gaiax.notarization.profile.infrastructure.config.MultiProfileTestProfile
import eu.gaiax.notarization.profile.infrastructure.rest.client.*
import io.quarkus.runtime.StartupEvent
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.infrastructure.Infrastructure
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Supplier


/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@Disabled
@TestProfile(NoSsiIssuanceV1Profile::class)
@QuarkusTestResource(MockOauth2ServerResource::class)
open class OnlySsiV2ClientTest {

    @Inject
    lateinit var sut: Set<ApiVersion>
    @Inject
    lateinit var v1: Optional<SsiIssuanceV1HttpClient>
    @Inject
    lateinit var v2: Optional<SsiIssuanceV2HttpClient>

    @Test
    fun verifyRevocationInitializedAfterTriggeringInitialization() {
        assertThat(sut, equalTo(setOf(ApiVersion.V2)))
    }

    @Test
    fun hasCorrectV1Client() {
        assertThat(v1.isEmpty, equalTo(true))
    }

    @Test
    fun hasCorrectV2Client() {
        assertThat(v2.isPresent, equalTo(true))
    }

}
