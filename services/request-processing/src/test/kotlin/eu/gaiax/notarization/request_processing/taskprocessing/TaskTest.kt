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
package eu.gaiax.notarization.request_processing.taskprocessing

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.http.QueryParameter
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSessionWithState
import eu.gaiax.notarization.request_processing.Helper.Companion.prepareSessionWithSubmittedNotarizationRequest
import eu.gaiax.notarization.request_processing.Helper.Companion.setSessionState
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.extensions.ExternalServicesWireMock
import eu.gaiax.notarization.request_processing.extensions.MockExternalServicesResource
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SessionTaskSummary
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogTaskCancel
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogTaskFail
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogTaskFinish
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogTaskStart
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockExternalServicesResource::class)
class TaskTest {
    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @ExternalServicesWireMock
    lateinit var wiremock: WireMockServer

    @ConfigProperty(name = "quarkus.http.port")
    var assignedPort: Int = 0

    fun stubConfig() {

    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug { " WireMock stub identity service request at URL: ${inRequest.absoluteUrl}" }
        logger.debug { " WireMock stub identity service request headers: \n${inRequest.headers}" }
        logger.debug { " WireMock stub identity service request body: \n${inRequest.bodyAsString}" }
        logger.debug { " WireMock stub identity service response body: \n${inResponse.bodyAsString}" }
        logger.debug { " WireMock stub identity service response headers: \n${inResponse.headers}" }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun startTask() {
        stubConfig()

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val sessionInfo = sessionWithNotarizationRequest.session
        //call fetch session to get taskids
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", sessionInfo.accessToken)
            .`when`().get(sessionInfo.location)
            .then()
            .statusCode(200)
            .extract()
            .path<Any>("tasks")
        val tasklist = Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)
            .extract().body().path<Any>("uri")

//        assertStateInStoredSession(sessionWithNotarizationRequest.session().id(), sessionFactory, NotarizationRequestState.ACCEPTED);
        withTransaction(
            sessionFactory
        ) { dbSession, transaction ->
            dbSession.find(
                Session::class.java, sessionWithNotarizationRequest.session.id
            )
                .call { s -> Mutiny.fetch(s.tasks) }
                .invoke { foundSession ->
                    val sessTask = foundSession!!.tasks!!.first { it.taskId == task.taskId }

                    MatcherAssert.assertThat(sessTask.taskId, CoreMatchers.`is`(task.taskId))
                    MatcherAssert.assertThat(
                        "Expected state started of SessionTask was not correct",
                        sessTask.running,
                        CoreMatchers.`is`(true)
                    )
                }
        }
        MatcherAssert.assertThat(
            auditTrailFor(sessionInfo.id, NotarizationRequestAction.TASK_START, sessionFactory),
            hasAuditEntries(auditLogTaskStart())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun cancelTaskWithSubmitableRequest() {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        cancelTask(sessionWithNotarizationRequest.session)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun cancelTaskWithCreatedRequest() {

        //use profile without preconTasks
        val sessionInfo = prepareSessionWithState(
            NotarizationRequestState.CREATED,
            sessionFactory,
            MockState.profileWithOnlyPreConditionId
        )
        cancelTask(sessionInfo)
    }

    @Throws(IllegalArgumentException::class)
    private fun cancelTask(sessionInfo: Helper.SessionInfo) {
        //var sessionInfo =sessionWithNotarizationRequest.session();
        //call fetch session to get taskids
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", sessionInfo.accessToken)
            .`when`().get(sessionInfo.location)
            .then()
            .statusCode(200)
            .extract()
            .path<Any>("tasks")
        val tasklist = Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionInfo.id)
            .header("token", sessionInfo.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)
        withTransaction(
            sessionFactory
        ) { dbSession: Mutiny.Session, transaction: Mutiny.Transaction ->
            dbSession.find(
                Session::class.java, sessionInfo.id
            )
                .call { s: Session? -> Mutiny.fetch(s!!.tasks) }
                .invoke { foundSession ->
                    val sessTask =
                        foundSession.tasks!!.first { t -> t.taskId!! == task.taskId }
                    MatcherAssert.assertThat(sessTask.taskId, CoreMatchers.`is`(task.taskId))
                    MatcherAssert.assertThat(
                        "Expected state of SessionTask was not correct",
                        sessTask.running,
                        CoreMatchers.`is`<Boolean>(true)
                    )
                }
        }
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionInfo.id)
            .header("token", sessionInfo.accessToken)
            .param("taskId", task.taskId.toString())
            .`when`()
            .delete(TASK_PATH)
            .then()
            .statusCode(204)
        withTransaction(
            sessionFactory
        ) { dbSession: Mutiny.Session, transaction ->
            dbSession.find(
                Session::class.java, sessionInfo.id
            )
                .call { s: Session? -> Mutiny.fetch(s!!.tasks) }
                .invoke { foundSession ->
                    val sessTask =
                        foundSession!!.tasks!!.first { t -> t.taskId!!.equals(task.taskId) }
                    MatcherAssert.assertThat(sessTask.taskId, CoreMatchers.`is`<UUID?>(task.taskId))
                    MatcherAssert.assertThat(
                        "Expected state of SessionTask was not correct",
                        sessTask.running,
                        CoreMatchers.`is`<Boolean>(false)
                    )
                }
        }
        MatcherAssert.assertThat(
            auditTrailFor(sessionInfo.id, NotarizationRequestAction.TASK_CANCEL, sessionFactory),
            hasAuditEntries(auditLogTaskCancel())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    @Throws(URISyntaxException::class)
    fun onlyPreconditionTasksStartableWhenNotYetFinished() {

        //starts with "some profile" -> which has preconditiontask identifiy and normal tasks for download
        val sessionWithNotarizationRequest =
            prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.someProfileId)
        val sessionInfo = sessionWithNotarizationRequest.session
        stubConfig()

        //call fetch session to get taskids
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", sessionInfo.accessToken)
            .`when`().get(sessionInfo.location)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .path<Any>("tasks")
        val tasklist = Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val uploadtask = tasklist.stream().filter { t -> t.name?.contains("upload") ?: false }
            .findAny().orElseThrow()

        //wrong task leads to 400
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", uploadtask.taskId.toString())
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(400)
        val identTask =
            tasklist.stream().filter { t -> t.name?.contains("identify") ?: false }
                .findAny().orElseThrow()
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", identTask.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)

        //hijack success url
        val successUri = wiremock.allServeEvents.stream()
            .map { e: ServeEvent -> e.request.queryParams }
            .map { e: Map<String, QueryParameter> -> e["success"] }
            .filter { v: QueryParameter? -> v!!.isPresent }
            .findFirst().get().values()[0]
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body("{}")
            .`when`()
            .post(asCallbackUrl(successUri))
            .then()
            .statusCode(204)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(sessionInfo.id, NotarizationRequestAction.TASK_FINISH_SUCCESS, sessionFactory),
            hasAuditEntries(auditLogTaskFinish())
        )
        //now starting the upload task should be fine
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", uploadtask.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    @Throws(URISyntaxException::class)
    fun failedTaskGetsAudit() {

        //starts with "some profile" -> which has preconditiontask identifiy and normal tasks for download
        val sessionWithNotarizationRequest =
            prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.someProfileId)
        val sessionInfo = sessionWithNotarizationRequest.session
        stubConfig()

        //call fetch session to get taskids
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", sessionInfo.accessToken)
            .`when`().get(sessionInfo.location)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .path<Any>("tasks")
        val tasklist = Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val identTask =
            tasklist.stream().filter { t -> t.name?.contains("identify") ?: false }
                .findAny().orElseThrow()
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", identTask.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)

        //hijack fail url
        val failUri = wiremock.allServeEvents.stream()
            .map { e: ServeEvent -> e.request.queryParams }
            .map { e: Map<String, QueryParameter> -> e["failure"] }
            .filter { v: QueryParameter? -> v!!.isPresent }
            .findFirst().get().values()[0]
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body("{}")
            .`when`()
            .post(asCallbackUrl(failUri))
            .then()
            .statusCode(204)
        MatcherAssert.assertThat(
            auditTrailFor(sessionInfo.id, NotarizationRequestAction.TASK_FINISH_FAIL, sessionFactory),
            hasAuditEntries(auditLogTaskFail())
        )
    }

    fun finishTaskOnlyWorksInCorrectState() {

        //starts with "some profile" -> which has preconditiontask identifiy and normal tasks for download
        val sessionWithNotarizationRequest =
            prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.someProfileId)
        val sessionInfo = sessionWithNotarizationRequest.session
        stubConfig()

        //call fetch session to get taskids
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", sessionInfo.accessToken)
            .`when`().get(sessionInfo.location)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .path<Any>("tasks")
        val tasklist = Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val identTask =
            tasklist.stream().filter { t -> t.name?.contains("identify") ?: false }
                .findAny().orElseThrow()
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", identTask.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)

        //hijack success url
        val successUri = wiremock.allServeEvents.stream()
            .map { e: ServeEvent -> e.request.queryParams }
            .map { e: Map<String, QueryParameter> -> e["success"] }
            .filter { v: QueryParameter? -> v!!.isPresent }
            .findFirst().get().values()[0]


        //change session state to non valid taskfinish state
        setSessionState(sessionInfo, NotarizationRequestState.READY_FOR_REVIEW, sessionFactory)
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body("{}")
            .`when`()
            .post(successUri)
            .then()
            .statusCode(400)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(sessionInfo.id, NotarizationRequestAction.TASK_FINISH_SUCCESS, sessionFactory),
            hasAuditEntries(auditLogTaskCancel())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    fun deleteSessionCancelsOngoingTasks() {
        stubConfig()
        val sessionWithNotarizationRequest =
            prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val session = sessionWithNotarizationRequest.session
        //call fetch session to get taskids
        var tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", session.accessToken)
            .`when`().get(session.location)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .path<Any>("tasks")

        //startall
        val tasklist = Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        tasklist.stream().filter { t -> t.name?.contains("upload") ?: false }
            .forEach { st: SessionTaskSummary ->
                RestAssured.given()
                    .contentType(ContentType.JSON)
                    .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
                    .header("token", sessionWithNotarizationRequest.session.accessToken)
                    .queryParam("taskId", st.taskId)
                    .`when`()
                    .post(TASK_PATH)
                    .then()
                    .statusCode(201)
            }

        //call fetch session to get them again
        tasks = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", session.accessToken)
            .`when`().get(session.location)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .path<Any>("tasks")
        Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
            .stream()
            .forEach { st: SessionTaskSummary -> MatcherAssert.assertThat(st.running, CoreMatchers.`is`(true)) }

        //cancel session
        RestAssured.given().contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, session.id)
            .header("token", session.accessToken)
            .`when`().delete(Helper.SUBMISSION_PATH)
            .then()
            .statusCode(204)

        //call fetch session to get them again
        tasks = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", session.accessToken)
            .`when`().get(session.location)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .path<Any>("tasks")
        Arrays.asList(*mapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
            .stream()
            .forEach { st: SessionTaskSummary -> MatcherAssert.assertThat(st.running, CoreMatchers.`is`(false)) }
    }

    companion object {
        const val TASK_PATH = "/api/v1/session/{sessionId}/task"
        private val random = Random()
        var mapper = ObjectMapper()
    }

    fun asCallbackUrl(rawUrl: String?): URL {
        return try {
            UriBuilder.fromUri(rawUrl).port(assignedPort).build().toURL()
        } catch (ex: MalformedURLException) {
            throw RuntimeException(ex)
        }
    }
}
