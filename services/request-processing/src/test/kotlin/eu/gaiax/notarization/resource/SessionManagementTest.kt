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
package eu.gaiax.notarization.resource

import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.util.*
import java.util.stream.Stream

/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
class SessionManagementTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun canCreateSession() {
        val response = RestAssured.given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(
                """
                    {"profileId":"${MockState.someProfileId.id}"}
                """.trimIndent()
            )
            .`when`().post("/api/v1/session")
            .then()
            .statusCode(201)
            .header("Location", CoreMatchers.containsString("/api/v1/session/"))
            .body(
                "token", CoreMatchers.`is`(
                    CoreMatchers.instanceOf<Any>(
                        String::class.java
                    )
                )
            )
            .body(
                "sessionId", CoreMatchers.`is`(
                    CoreMatchers.instanceOf<Any>(
                        String::class.java
                    )
                )
            )
            .extract()
        val actualLocation = response.header("Location")
        val actualToken = response.path<Any>("sessionId").toString()
        MatcherAssert.assertThat(actualLocation, CoreMatchers.endsWith(actualToken))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun canFetchCreatedSession() {
        val sessionInfo = createNewSession()
        RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", sessionInfo.accessToken)
            .`when`().get(sessionInfo.location)
            .then()
            .statusCode(200)
            .body(
                "sessionId", CoreMatchers.equalTo<String>(sessionInfo.id),
                "tasks", Matchers.hasSize<Any>(Matchers.greaterThan<Int>(0))
            )
    }

    @ParameterizedTest
    @MethodSource("allNamedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun givenSessionThenFetchedStateIsCorrect(given: NotarizationRequestState, expected: String) {
        val givenSession = SessionIdentifier.any()
        createSessionWithState(givenSession, given)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(SESSION_VARIABLE, givenSession.id)
            .header("token", givenSession.accessToken)
            .`when`()[SESSION_PATH]
            .then()
            .statusCode(200)
            .body(
                "state", CoreMatchers.equalTo<String>(expected),
                "sessionId", CoreMatchers.equalTo<String>(givenSession.id)
            )
    }

    private fun createSessionWithState(sessionId: SessionIdentifier, state: NotarizationRequestState) {
        withTransaction<Void>(sessionFactory!!) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            val newSession = Session()
            newSession.id = sessionId.id
            newSession.profileId = ProfileId(MockState.someProfileId.id)
            newSession.accessToken = sessionId.accessToken
            newSession.state = state
            session.persist(newSession)
        }
    }

    private fun createNewSession(): SessionInfo {
        val response = RestAssured.given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(
                """
                    {"profileId":"${MockState.someProfileId.id}"}
                """.trimIndent()
            )
            .`when`().post("/api/v1/session")
            .then()
            .statusCode(201).extract()
        return SessionInfo(
            response.path<String>("sessionId"),
            response.header("Location"),
            response.path<String>("token")
        )
    }

    class SessionInfo(
        val id: String,
        val location: String,
        val accessToken: String
    )
    class SessionIdentifier(
        val id: String,
        val accessToken: String
    ) {
        companion object {
            fun any(): SessionIdentifier {
                return SessionIdentifier(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString()
                )
            }
        }
    }

    companion object {
        private const val SESSION_VARIABLE = "sessionId"
        private const val SESSION_PATH = "/api/v1/session/{sessionId}"
        @JvmStatic
        fun allNamedStates(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(NotarizationRequestState.ACCEPTED, NotarizationRequestState.Name.ACCEPTED),
                Arguments.of(NotarizationRequestState.EDITABLE, NotarizationRequestState.Name.EDITABLE),
                Arguments.of(NotarizationRequestState.ISSUED, NotarizationRequestState.Name.ISSUED),
                Arguments.of(NotarizationRequestState.TERMINATED, NotarizationRequestState.Name.TERMINATED),
                Arguments.of(NotarizationRequestState.PENDING_DID, NotarizationRequestState.Name.PENDING_DID),
                Arguments.of(NotarizationRequestState.READY_FOR_REVIEW, NotarizationRequestState.Name.READY_FOR_REVIEW),
                Arguments.of(NotarizationRequestState.SUBMITTABLE, NotarizationRequestState.Name.SUBMITTABLE),
                Arguments.of(NotarizationRequestState.WORK_IN_PROGRESS, NotarizationRequestState.Name.WORK_IN_PROGRESS)
            )
        }
    }
}
