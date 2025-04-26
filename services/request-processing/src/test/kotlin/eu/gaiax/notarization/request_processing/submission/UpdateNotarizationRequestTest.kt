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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.DataGen.createRandomJsonData
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.setSessionState
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogCreateSession
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogSubmit
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogUpdate
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.time.Duration
import java.util.function.BiFunction
import java.util.function.Function
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
class UpdateNotarizationRequestTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @ParameterizedTest
    @MethodSource("updateableStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canUpdateRequest(state: NotarizationRequestState?) {
        val sessionWithSubmittedNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        setSessionState(sessionWithSubmittedNotarizationRequest.session, state, sessionFactory)
        val dataToUpdate = createRandomJsonData()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithSubmittedNotarizationRequest.session.id)
            .header("token", sessionWithSubmittedNotarizationRequest.session.accessToken)
            .body(dataToUpdate)
            .`when`()
            .put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(204)
            .extract()
        assertDataOfNotRequestInDB(sessionWithSubmittedNotarizationRequest.session, dataToUpdate)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithSubmittedNotarizationRequest.session.id,
                NotarizationRequestAction.UPDATE,
                sessionFactory
            ),
            hasAuditEntries(auditLogUpdate())
        )
    }

    @ParameterizedTest
    @MethodSource("nonUpdateableStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun cannotUpdateRequest(state: NotarizationRequestState?) {
        val sessionWithSubmittedNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        setSessionState(sessionWithSubmittedNotarizationRequest.session, state, sessionFactory)
        val dataToUpdate = createRandomJsonData()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithSubmittedNotarizationRequest.session.id)
            .header("token", sessionWithSubmittedNotarizationRequest.session.accessToken)
            .body(dataToUpdate)
            .`when`()
            .put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
            .extract()

        //data not changed assertion proofs data generated in prepareSession
        assertDataOfNotRequestInDB(
            sessionWithSubmittedNotarizationRequest.session,
            sessionWithSubmittedNotarizationRequest.notarizationRequest.data!!
        )
        MatcherAssert.assertThat(
            auditTrailFor(
                sessionWithSubmittedNotarizationRequest.session.id,
                NotarizationRequestAction.UPDATE,
                sessionFactory
            ),
            hasAuditEntries(auditLogUpdate().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun invalidTokenDetectedInUpdateRequest() {
        val sessionWithSubmittedNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithSubmittedNotarizationRequest.session.id)
            .header("token", sessionWithSubmittedNotarizationRequest.session.accessToken + "INVALID")
            .body(createRandomJsonData())
            .`when`()
            .put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(401)
            .extract()

        //data not changed assertion proofs data generated in prepareSession
        assertDataOfNotRequestInDB(
            sessionWithSubmittedNotarizationRequest.session,
            sessionWithSubmittedNotarizationRequest.notarizationRequest.data!!
        )
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithSubmittedNotarizationRequest.session.id,
                NotarizationRequestAction.UPDATE,
                sessionFactory
            ),
            hasAuditEntries(auditLogUpdate().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun noTokenDetectedInUpdateRequest() {
        val sessionWithSubmittedNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithSubmittedNotarizationRequest.session.id)
            .body(createRandomJsonData())
            .`when`()
            .put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
            .extract()

        //data not changed assertion proofs data generated in prepareSession
        assertDataOfNotRequestInDB(
            sessionWithSubmittedNotarizationRequest.session,
            sessionWithSubmittedNotarizationRequest.notarizationRequest.data!!
        )
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithSubmittedNotarizationRequest.session.id,
                NotarizationRequestAction.UPDATE,
                sessionFactory
            ),
            hasAuditEntries(auditLogUpdate().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun emptyTokenDetectedInUpdateRequest() {
        val sessionWithSubmittedNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithSubmittedNotarizationRequest.session.id)
            .header("token", "")
            .body(createRandomJsonData())
            .`when`().put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(401)
            .extract()

        //data not changed assertion proofs data generated in prepareSession
        assertDataOfNotRequestInDB(
            sessionWithSubmittedNotarizationRequest.session,
            sessionWithSubmittedNotarizationRequest.notarizationRequest.data!!
        )
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithSubmittedNotarizationRequest.session.id,
                NotarizationRequestAction.UPDATE,
                sessionFactory
            ),
            hasAuditEntries(auditLogUpdate().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenInvalidJsonLdThenCannotUpdateRequest() {
        val givenSession = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val inputInvalidJsonld =
            asJson(
            """
               {
                 "@context": "https://schema.org/ContextDoesNotExistHere",
                 "@type": "Person",
                 "name": "Jane Doe",
                 "jobTitle": "Professor",
                 "telephone": "(425) 123-4567",
                 "url": "http://www.janedoe.com"
               }

            """.trimIndent())
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, givenSession.session.id)
            .header("token", givenSession.session.accessToken)
            .body(inputInvalidJsonld)
            .`when`()
            .put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(givenSession.session.id, NotarizationRequestAction.UPDATE, sessionFactory),
            hasAuditEntries(
                auditLogCreateSession(),
                auditLogUpdate().httpStatus(400)
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00023")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenValidJsonLdThenCanUpdateRequest() {
        val givenSession = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val inputValidJsonld =
            asJson(
                """
                   {
                     "@context": "https://schema.org/",
                     "@type": "Person",
                     "name": "Jane Doe",
                     "jobTitle": "Professor",
                     "telephone": "(425) 123-4567",
                     "url": "http://www.janedoe.com"
                   }
                """.trimIndent()
            )
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, givenSession.session.id)
            .header("token", givenSession.session.accessToken)
            .body(inputValidJsonld)
            .`when`()
            .put(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(204)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(givenSession.session.id, NotarizationRequestAction.UPDATE, sessionFactory),
            hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit(),
                auditLogUpdate()
            )
        )
    }

    private fun assertDataOfNotRequestInDB(sess: Helper.SessionInfo, expected: JsonNode) {
        withTransactionAsync(
            sessionFactory
        ) { session, tx ->
            session.find(
                Session::class.java, sess.id
            )
        }.map { session -> session.request!!.data }
            .invoke { data -> MatcherAssert.assertThat(data, CoreMatchers.`is`(expected.toString())) }
            .await().atMost(Duration.ofSeconds(1))
    }

    companion object {

        @JvmStatic
        fun updateableStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.EDITABLE
            )
        }

        @JvmStatic
        fun nonUpdateableStates(): Stream<NotarizationRequestState> {
            val updateable = updateableStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !updateable.contains(s) }
        }

        private fun asJson(value: String): JsonNode {
            return try {
                ObjectMapper().readTree(value)
            } catch (ex: JsonProcessingException) {
                throw RuntimeException(ex)
            }
        }
    }
}
