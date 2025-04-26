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
package eu.gaiax.notarization.request_processing.management

import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState.Notary
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasItems
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junitpioneer.jupiter.ReportEntry
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@Tag("security")
@QuarkusTestResource(
    MockServicesLifecycleManager::class
)
class FetchAvailableRequestPermissionsTest {
    private var totalRecordsInserted = 100

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @BeforeEach
    fun setup() {
        pruneDB()
        totalRecordsInserted = 0
        IntStream.range(0, 3).forEach { c: Int ->
            for (currentProfile in MockState.profileIds) {
                totalRecordsInserted++
                createRequestWithStateInDB(NotarizationRequestState.READY_FOR_REVIEW, currentProfile)
            }
        }
    }

    private fun pruneDB() {
        withTransactionAsync(sessionFactory) { session, tx ->
            session.createMutationQuery(
                "delete NotarizationRequest"
            ).executeUpdate()
        }
            .await().indefinitely()
    }

    private fun createRequestWithStateInDB(state: NotarizationRequestState, profile: ProfileId): String {
        val id = UUID.randomUUID()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = UUID.randomUUID().toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            val nr = NotarizationRequest()
            nr.id = id
            nr.session = sess
            session.persist(sess).chain { _ -> session.persist(nr) }
        }.await().indefinitely()
        return id.toString()
    }

    @ParameterizedTest(name = "{index} Notary {0} accesses correct requests")
    @ArgumentsSource(
        AllMockNotariesArgumentsProvider::class
    )
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canOnlyFetchRequestsByProfileRoles(subject: UUID?) {
        val givenNotary = MockState.notariesBySubject[subject]!!
        val expectedRoles = givenNotary.roles.map { r -> r.id }
        val unexpectedRoles = givenNotary.knownUnauthorizedRoles().map { r -> r.id }
        val response = RestAssured.given()
            .header("Authorization", givenNotary.bearerValue())
            .queryParam("offset", 0)
            .queryParam("limit", totalRecordsInserted)
            .`when`()[REQUESTS_PATH]
            .then()
            .statusCode(200)
            .extract()
        val resultProfileIds = HashSet(response.jsonPath().getList<Any>("notarizationRequests.profileId"))
        assertThat(
            resultProfileIds,
            Matchers.describedAs(
                "Notary %0 to have access to profiles %1",
                hasItems(*expectedRoles.toTypedArray()),
                givenNotary.name,
                expectedRoles
            )
        )
        assertThat(
            resultProfileIds,
            Matchers.describedAs(
                "Notary %0 not to have access to profiles %1",
                Matchers.not(hasItems(*unexpectedRoles.toTypedArray())),
                givenNotary.name,
                unexpectedRoles
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canOnlyFetchRequestsByProfileRoleNotMatchingIdentity() {
        val givenNotary = MockState.notary5
        val expectedRoles = givenNotary.roles.map { r -> r.id }
        val unexpectedRoles = givenNotary.knownUnauthorizedRoles().map { r -> r.id }
        val response = RestAssured.given()
            .header("Authorization", givenNotary.bearerValue())
            .queryParam("offset", 0)
            .queryParam("limit", totalRecordsInserted)
            .`when`()[REQUESTS_PATH]
            .then()
            .statusCode(200)
            .extract()
        val resultProfileIds = HashSet(response.jsonPath().getList<Any>("notarizationRequests.profileId"))
        assertThat(
            resultProfileIds,
            Matchers.describedAs(
                "Notary %0 to have access to profiles %1",
                hasItems(*expectedRoles.toTypedArray()),
                givenNotary.name,
                expectedRoles
            )
        )
        assertThat(
            resultProfileIds,
            Matchers.describedAs(
                "Notary %0 not to have access to profiles %1",
                Matchers.not(hasItems(*unexpectedRoles.toTypedArray())),
                givenNotary.name,
                unexpectedRoles
            )
        )
    }

    class AllMockNotariesArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            /*
             * The use of subject is a workaround to avoid the de-/serialization of
                types currently not supported by x-stream.

                Waiting for version 1.5
                https://github.com/x-stream/xstream/issues/168
             */
            return MockState.notaries.stream().map { n: Notary -> Arguments.of(n.subject) }
        }
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}"
    }
}
