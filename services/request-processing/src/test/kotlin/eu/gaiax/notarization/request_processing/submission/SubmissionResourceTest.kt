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
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.createSubmitNotarizationRequest
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSession
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSessionWithState
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SubmitNotarizationRequest
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogCreateSession
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogSubmit
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.util.*

@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
open class SubmissionResourceTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
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
        val sessionId = response.path<Any>("sessionId").toString()
        MatcherAssert.assertThat(actualLocation, CoreMatchers.endsWith(sessionId))
        MatcherAssert.assertThat(
            auditTrailFor(sessionId, NotarizationRequestAction.CREATE_SESSION, 1, sessionFactory), hasAuditEntries(
                auditLogCreateSession()
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun createSessionUsingProfileWithOptionalPreconditionTasksEndsInSubmittableSession() {
        val session = prepareSession(MockState.profileWithOptionalPreConditionId)
        RestAssured.given()
            .accept(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()[Helper.SESSION_PATH]
            .then()
            .body("state", CoreMatchers.`is`("submittable"))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun createSessionUsingProfileWithPreconditionLeadsToSessionInCreatedState() {
        val session = prepareSession(MockState.profileId4)
        RestAssured.given()
            .accept(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`()[Helper.SESSION_PATH]
            .then()
            .body("state", CoreMatchers.`is`("created"))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun canNotCreateSessionWithoutProfile() {
        RestAssured.given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`().post("/api/v1/session")
            .then()
            .statusCode(400)
            .extract()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun canSubmitRequestGivenNewSessionInfo() {
        val session = prepareSessionWithState(NotarizationRequestState.SUBMITTABLE, sessionFactory)
        val inputRequest = createSubmitNotarizationRequest()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(inputRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(201)
            .header("Location", CoreMatchers.`is`(CoreMatchers.notNullValue()))
        assertDataExistsInDB(session, inputRequest)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.SUBMIT, 1, sessionFactory), hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit()
            )
        )
    }

    private fun assertDataExistsInDB(session: Helper.SessionInfo, inputRequest: SubmitNotarizationRequest) {
        val actual = getSession(session)
        MatcherAssert.assertThat(actual.request!!.data, CoreMatchers.`is`(inputRequest.data.toString()))
    }

    private fun getSession(sessionInfo: Helper.SessionInfo): Session {
        return withTransaction(
            sessionFactory
        ) { dbSession, transaction ->
            dbSession.find(
                Session::class.java, sessionInfo.id
            )
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun givenSubmissionThenHasCorrectState() {
        val inputState = NotarizationRequestState.SUBMITTABLE
        val session = prepareSessionWithState(inputState, sessionFactory)
        val inputRequest = createSubmitNotarizationRequest()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(inputRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(201)
        val actual = getSession(session)
        MatcherAssert.assertThat(actual, CoreMatchers.not(CoreMatchers.nullValue()))
        MatcherAssert.assertThat(actual.state, CoreMatchers.equalTo(NotarizationRequestState.EDITABLE))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenMissingTokenThenCannotSubmitRequest() {
        val session = prepareSession()
        val notarizationRequest = createSubmitNotarizationRequest()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.SUBMIT, 1, sessionFactory), hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit().httpStatus(400)
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenInvalidTokenThenCannotSubmitRequest() {
        val inputInvalidToken = UUID.randomUUID().toString()
        val session = prepareSession()
        val notarizationRequest = createSubmitNotarizationRequest()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .header("token", inputInvalidToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.SUBMIT, 1, sessionFactory),
            hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit().httpStatus(401)
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenInvalidSessionThenCannotSubmitRequest() {
        val invalidSessionID = UUID.randomUUID().toString()
        val session = prepareSession()
        val notarizationRequest = createSubmitNotarizationRequest()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, invalidSessionID)
            .body(notarizationRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(404)
        MatcherAssert.assertThat(
            auditTrailFor(invalidSessionID, NotarizationRequestAction.SUBMIT, 1, sessionFactory),
            CoreMatchers.allOf(
                hasAuditEntries(auditLogSubmit().httpStatus(404)),
                CoreMatchers.not(hasAuditEntries(auditLogCreateSession()))
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenInvalidContextJsonLdThenCannotSubmitRequest() {
        val session = prepareSessionWithState(NotarizationRequestState.SUBMITTABLE, sessionFactory)
        val notarizationRequest = createSubmitNotarizationRequest(
            asJson(
                """
                    {
                     "@context": "https://schema.org/ContextDoesNotExistHere",
                     "@type": "ContextDoesNotExistHere",
                     "name": "Jane Doe",
                     "jobTitle": "Professor",
                     "telephone": "(425) 123-4567",
                     "url": "http://www.janedoe.com"
                   }
                """.trimIndent()
            )
        )
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.SUBMIT, 1, sessionFactory),
            hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit().httpStatus(400)
            )
        )
    }

    @Test
    @Disabled(value = "JSON-LD processing will be reworked later")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenInvalidFieldsJsonLdThenCannotSubmitRequest() {
        val session = prepareSessionWithState(NotarizationRequestState.SUBMITTABLE, sessionFactory)
        val notarizationRequest = createSubmitNotarizationRequest(
            asJson(
                """
                    {
                     "@context":["https://www.w3.org/2018/credentials/v1","https://w3id.org/vc/status-list/2021/v1"],
                     "@type": "Person",
                     "name": "Jane Doe",
                     "jobTitle": "Professor",
                     "telephone": "(425) 123-4567",
                     "url": "http://www.janedoe.com",
                     "dummy": 32
                   }
                """.trimIndent()
            )
        )
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.SUBMIT, 1, sessionFactory),
            hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit().httpStatus(400)
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun givenValidJsonLdThenCanSubmitRequest() {
        val session = prepareSessionWithState(NotarizationRequestState.SUBMITTABLE, sessionFactory)
        val notarizationRequest = createSubmitNotarizationRequest(
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
        )
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(201)
        MatcherAssert.assertThat(
            auditTrailFor(session.id, NotarizationRequestAction.SUBMIT, 1, sessionFactory), hasAuditEntries(
                auditLogCreateSession(),
                auditLogSubmit()
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00022")
    fun givenUsedSessionThenCannotSubmitRequest() {
        val session = prepareSession(MockState.profileId1)
        val notarizationRequest = createSubmitNotarizationRequest()
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(201)
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .body(notarizationRequest)
            .header("token", session.accessToken)
            .`when`().post(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(400)
    }

    companion object {
        private fun asJson(value: String): JsonNode {
            return try {
                ObjectMapper().readTree(value)
            } catch (ex: JsonProcessingException) {
                throw RuntimeException(ex)
            }
        }
    }
}
