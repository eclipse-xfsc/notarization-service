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
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.api.profile.*
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.gaiax.notarization.profile.domain.entity.PersistantProfile
import eu.gaiax.notarization.profile.domain.entity.ProfileDid
import eu.gaiax.notarization.profile.infrastructure.MockOauth2ServerResource
import eu.gaiax.notarization.profile.infrastructure.config.ProfileMatcher.Companion.equalsProfile
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.runtime.StartupEvent
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.quarkus.vertx.VertxContextSupport
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@TestProfile(SingleProfileTestProfile::class)
@QuarkusTestResource(MockOauth2ServerResource::class)
class SingleProfileTest {
    @Inject
    lateinit var profileService: ConfigBackedProfileService

    @Inject
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        VertxContextSupport.subscribeAndAwait { ->
            Panache.withTransaction { ->
                ProfileDid.deleteAll()
                    .chain { _ ->
                        PersistantProfile.deleteAll()
                    }
            }
        }
        profileService.onStartup(StartupEvent())
    }

    @Test
    @RunOnVertxContext
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    fun hasSingleProfile(asserter: UniAsserter) {
        asserter.assertThat({
            profileService.list(0, 10, SortDirection.Ascending)
        }, { results ->
            MatcherAssert.assertThat(
                results.items, Matchers.hasSize(1)
            )
        })
    }

    @Test
    @RunOnVertxContext
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    fun hasExpectedItem(asserter: UniAsserter) {
        asserter.assertThat({
            profileService.list(0, 10, SortDirection.Ascending)
        }, { results ->
            MatcherAssert.assertThat(
                results.items, Matchers.hasItem(equalsProfile(SingleProfileTestProfile.singleValue))
            )
        })
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    fun canFetchAllProfiles() {
        val expected = SingleProfileTestProfile.singleValue
        RestAssured.given()
            .accept(ContentType.JSON)
            .`when`()[PROFILES_ALL_PATH]
            .then()
            .statusCode(200)
            .body("items", Matchers.hasSize<Any>(1))
            .body("items[0].id", Matchers.equalTo(expected.id))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00021")
    @Throws(JsonProcessingException::class)
    fun canFetchSingleProfile() {
        val resultResponse = RestAssured.given()
            .accept(ContentType.JSON)
            .pathParam(PROFILE_VARIABLE, SingleProfileTestProfile.singleValue.id)
            .`when`()[PROFILE_SINGLE_PATH]
            .then()
            .statusCode(200)
            .extract()
        val resultProfiles: Profile = objectMapper.readValue(
            resultResponse.asString(),
            object : TypeReference<Profile>() {})

        MatcherAssert.assertThat(resultProfiles, equalsProfile(SingleProfileTestProfile.singleValue))
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun cannotFetchUnknownProfile() {
        val inputUnknownId = SingleProfileTestProfile.singleValue.id + "unknown"
        RestAssured.given()
            .accept(ContentType.JSON)
            .pathParam(PROFILE_VARIABLE, inputUnknownId)
            .`when`()[PROFILE_SINGLE_PATH]
            .then()
            .statusCode(404)
    }

    companion object {
        private const val PROFILE_VARIABLE = "profileId"
        private const val PROFILE_SINGLE_PATH = "/api/v1/profiles/{profileId}"
        private const val PROFILES_ALL_PATH = "/api/v1/profiles"
    }
}
