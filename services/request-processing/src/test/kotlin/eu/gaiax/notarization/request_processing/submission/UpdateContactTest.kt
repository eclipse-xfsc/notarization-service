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
package eu.gaiax.notarization.request_processing.submission

import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
class UpdateContactTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00027")
    fun canMarkReadyOnlyWithFulfilledTasks() {
        val session = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory).session
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()
            .post(MARK_READY_PATH)
            .then()
            .statusCode(400)

        //simulate ready tasks
        withTransaction(sessionFactory) { sess, tx ->
            sess.createMutationQuery("update SessionTask st set st.fulfilled = true").executeUpdate()
        }
        RestAssured.given() //.contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .body("emil@email.de")
            .`when`()
            .put(PATH)
            .then()
            .statusCode(204)
    }

    companion object {
        private const val MARK_READY_PATH = Helper.SUBMISSION_PATH + "/ready"
        const val PATH = "/api/v1/session/{sessionId}/updateContact"
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.EDITABLE
            )
        }

        fun notAllowedStates(): Stream<NotarizationRequestState> {
            val updateable = allowedStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !updateable.contains(s) }
        }
    }
}
