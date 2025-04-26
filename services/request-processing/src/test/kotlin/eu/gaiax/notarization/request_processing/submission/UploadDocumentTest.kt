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
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.google.common.io.CharStreams
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.request_processing.DataGen.createRandomJsonData
import eu.gaiax.notarization.request_processing.DataGen.genBytes
import eu.gaiax.notarization.request_processing.DataGen.genString
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.DocumentId
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.services.SignatureVerifierService
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.DocumentUploadByLink
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SessionTaskSummary
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.*
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditFetchDocument
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditUploadDocument
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.InjectMock
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hibernate.reactive.mutiny.Mutiny
import org.jboss.logging.Logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junitpioneer.jupiter.ReportEntry
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.BiFunction

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
class UploadDocumentTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    var encoder = Base64.getUrlEncoder()

    @InjectMock
    lateinit var mockSignatureVerifier: SignatureVerifierService
    @BeforeEach
    fun setup() {
        downloadServer!!.resetAll()
        downloadServer!!.stubFor(
            WireMock.get(WireMock.urlMatching(".+"))
                .atPriority(100)
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(404)
                )
        )
        downloadServer!!.addMockServiceRequestListener { inRequest: Request, inResponse: Response ->
            requestReceived(
                inRequest,
                inResponse
            )
        }
    }

    protected fun requestReceived(inRequest: Request, inResponse: Response) {
        logger.debug{"Download WireMock request at URL: ${inRequest.absoluteUrl}" }
        logger.debug{"Download WireMock request headers: \n${inRequest.headers}" }
        logger.debug{"Download WireMock response headers: \n${inResponse.headers}" }
        logger.debug{"Download WireMock response body: \n${inResponse.bodyAsString}" }
    }

    class DocumentBuilder(
        private val document: DocumentUploadByLink,
        private val content: String?,
        private val isDownloadable: Boolean
    ) {
        fun build(): DocumentUploadByLink {
            if (isDownloadable) {
                val actualContent = content ?: createRandomJsonData().asText()
                downloadServer!!.stubFor(
                    WireMock.get(WireMock.urlEqualTo(document.location!!.path))
                        .atPriority(1)
                        .willReturn(
                            WireMock.aResponse()
                                .withBody(actualContent)
                                .withStatus(200)
                        )
                )
            }
            return document
        }

        fun isDownloadable(): DocumentBuilder {
            return DocumentBuilder(document, content, true)
        }

        fun unreachable(): DocumentBuilder {
            return DocumentBuilder(document, content, false)
        }

        fun withContent(content: String?): DocumentBuilder {
            return DocumentBuilder(document, content, isDownloadable).isDownloadable()
        }
    }

    @Test
    @Throws(JsonProcessingException::class, URISyntaxException::class, IOException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canUploadDocument(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val expectedReport = someVerificationReport()
        val inputUpload = textFile(baseTempDir)
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(Files.readAllBytes(inputUpload.content))))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        val token = sessionWithNotarizationRequest.session.accessToken

        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)

        //upload document via form
        RestAssured.given().spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(204)

        //call finish
        RestAssured.given()
            .pathParam("taskId", task.taskId)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/finishTask")
            .then()
            .statusCode(204)
        assertDocumentInDb(
            sessionWithNotarizationRequest.session,
            inputUpload,
            encoder.encodeToString(expectedReport.toByteArray())
        )
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.UPLOAD_DOCUMENT,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditUploadDocument())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @Throws(JsonProcessingException::class, URISyntaxException::class, IOException::class)
    fun canNotUploadDocumentWithoutStartingTask(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val inputUpload = textFile(baseTempDir)
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        val token = sessionWithNotarizationRequest.session.accessToken

        //upload document via form
        RestAssured.given()
            .spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(404)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @Throws(JsonProcessingException::class, URISyntaxException::class)
    fun canNOTUploadDocumentWithWrongToken(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val inputUpload = textFile(baseTempDir)
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        var token = sessionWithNotarizationRequest.session.accessToken

        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)
        token = "INVALID"

        //upload document via form
        RestAssured.given()
            .spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(401)
    }

    @Test
    @Throws(JsonProcessingException::class, URISyntaxException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canUploadDocumentByLink() {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val givenRawDocument = someRawDocument()
        val expectedReport = someVerificationReport()
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(givenRawDocument)))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
        val docUp = someDownloadableDocument().withContent(givenRawDocument).build()
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()

        //startTask
        RestAssured.given()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)


        //upload document via link
        RestAssured.given()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionInfo.accessToken)
            .pathParam("taskId", task.taskId)
            .body(docUp)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/uploadByLink")
            .then()
            .statusCode(204)

        //call finish
        RestAssured.given()
            .pathParam("taskId", task.taskId)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionInfo.accessToken)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/finishTask")
            .then()
            .statusCode(204)
        assertDocumentInDb(
            sessionWithNotarizationRequest.session,
            docUp,
            givenRawDocument.toByteArray(),
            encoder.encodeToString(expectedReport.toByteArray())
        )
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.UPLOAD_DOCUMENT,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditUploadDocument())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @Throws(JsonProcessingException::class, URISyntaxException::class)
    fun handleUnreachableDownloads() {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val givenRawDocument = someRawDocument()
        val expectedReport = someVerificationReport()
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(givenRawDocument)))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
        val docUp = someDownloadableDocument().withContent(givenRawDocument).unreachable().build()
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()

        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)


        //upload document via link
        RestAssured.given()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionInfo.accessToken)
            .pathParam("taskId", task.taskId)
            .body(docUp)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/uploadByLink")
            .then()
            .statusCode(400)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @Throws(JsonProcessingException::class, URISyntaxException::class)
    fun canNOTUploadDocumentByLinkWithWrongToken() {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val givenRawDocument = someRawDocument()
        val expectedReport = someVerificationReport()
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(givenRawDocument)))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
        val docUp = someDownloadableDocument().withContent(givenRawDocument).build()
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()


        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)


        //upload document via link
        RestAssured.given()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", "INVALID")
            .pathParam("taskId", task.taskId)
            .body(docUp)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/uploadByLink")
            .then()
            .statusCode(401)
    }

    @Test
    @Throws(JsonProcessingException::class, URISyntaxException::class, IOException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canFetchUploadedDocument(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val expectedReport = someVerificationReport()
        val inputUpload = textFile(baseTempDir)
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(Files.readAllBytes(inputUpload.content))))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        val token = sessionWithNotarizationRequest.session.accessToken
        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)

        //upload document via form
        RestAssured.given()
            .spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(204)
        RestAssured.given()
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()["/api/v1/document/{sessionId}/{documentId}"]
            .then()
            .body("id", CoreMatchers.`is`<String>(inputUpload.id!!.id.toString()))
            .body("title", CoreMatchers.`is`<String?>(inputUpload.title))
            .body("shortDescription", CoreMatchers.`is`<String?>(inputUpload.shortDescription))
            .body("longDescription", CoreMatchers.`is`<String?>(inputUpload.longDescription))
            .statusCode(200)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.FETCH_DOCUMENT,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditFetchDocument())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @Throws(JsonProcessingException::class, URISyntaxException::class, IOException::class)
    fun canNotFetchUploadedDocumentWithInvalidToken(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val expectedReport = someVerificationReport()
        val inputUpload = textFile(baseTempDir)
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(Files.readAllBytes(inputUpload.content))))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        val token = sessionWithNotarizationRequest.session.accessToken
        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)

        //upload document via form
        RestAssured.given()
            .spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(204)
        RestAssured.given()
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", "INVALID")
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()["/api/v1/document/{sessionId}/{documentId}"]
            .then()
            .statusCode(401)
    }

    @Test
    @Throws(JsonProcessingException::class, URISyntaxException::class, IOException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canDeleteUploadedDocument(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val expectedReport = someVerificationReport()
        val inputUpload = textFile(baseTempDir)
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(Files.readAllBytes(inputUpload.content))))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        val token = sessionWithNotarizationRequest.session.accessToken
        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)

        //upload document via form
        RestAssured.given()
            .spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(204)

        //doc is there
        RestAssured.given()
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()["/api/v1/document/{sessionId}/{documentId}"]
            .then()
            .body("id", CoreMatchers.`is`<String>(inputUpload.id!!.id.toString()))
            .body("title", CoreMatchers.`is`<String?>(inputUpload.title))
            .body("shortDescription", CoreMatchers.`is`<String?>(inputUpload.shortDescription))
            .body("longDescription", CoreMatchers.`is`<String?>(inputUpload.longDescription))
            .statusCode(200)

        //delete
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()
            .delete("/api/v1/document/{sessionId}/{documentId}")
            .then()
            .statusCode(204)

        //is gone
        RestAssured.given()
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()["/api/v1/document/{sessionId}/{documentId}"]
            .then()
            .statusCode(404)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.FETCH_DOCUMENT,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditFetchDocument())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @Throws(JsonProcessingException::class, URISyntaxException::class, IOException::class)
    fun canNotDeleteUploadedDocumentWithInvalidToken(@TempDir baseTempDir: Path) {

        //use profile without preconTasks
        val sessionWithNotarizationRequest =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.profileId1)
        val expectedReport = someVerificationReport()
        val inputUpload = textFile(baseTempDir)
        Mockito.`when`(mockSignatureVerifier.verify(matchesInputStream(Files.readAllBytes(inputUpload.content))))
            .thenReturn(Uni.createFrom().item(expectedReport.toByteArray()))
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
        val tasklist = Arrays.asList(*objectMapper.convertValue(tasks, Array<SessionTaskSummary>::class.java))
        val task = tasklist.stream()
            .findAny().orElseThrow()
        val token = sessionWithNotarizationRequest.session.accessToken
        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", token)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(START_TASK_PATH)
            .then()
            .statusCode(201)

        //upload document via form
        RestAssured.given()
            .spec(inputUpload.asSpec())
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .pathParam("taskId", task.taskId)
            .header("token", token)
            .`when`()
            .post("/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(204)

        //doc is there
        RestAssured.given()
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()["/api/v1/document/{sessionId}/{documentId}"]
            .then()
            .body("id", CoreMatchers.`is`<String>(inputUpload.id!!.id.toString()))
            .body("title", CoreMatchers.`is`<String?>(inputUpload.title))
            .body("shortDescription", CoreMatchers.`is`<String?>(inputUpload.shortDescription))
            .body("longDescription", CoreMatchers.`is`<String?>(inputUpload.longDescription))
            .statusCode(200)

        //delete
        RestAssured.given()
            .pathParam(Helper.SESSION_VARIABLE, sessionWithNotarizationRequest.session.id)
            .header("token", "INVALID")
            .header("Accept", MediaType.APPLICATION_JSON)
            .pathParam("documentId", inputUpload.id!!.id)
            .`when`()
            .delete("/api/v1/document/{sessionId}/{documentId}")
            .then()
            .statusCode(401)
    }

    private fun assertDocumentInDb(
        sessionInfo: Helper.SessionInfo,
        documentUpload: UploadDescription,
        verificationReport: String
    ) {
        withTransaction(
            sessionFactory
        ) { session, tx ->
            session.find(
                Session::class.java, sessionInfo.id
            )
                .chain { found -> Mutiny.fetch(found.documents) }
                .invoke { docs ->
                    MatcherAssert.assertThat(
                        docs, CoreMatchers.hasItem<Any>(
                            CoreMatchers.allOf<Any>(
                                FieldMatcher.Companion.hasField<Any, UUID>(
                                    "id", CoreMatchers.`is`<UUID>(
                                        documentUpload.id!!.id
                                    )
                                ),
                                FieldMatcher.Companion.hasField<Any, ByteArray>(
                                    "content",
                                    CoreMatchers.`is`<ByteArray>(documentUpload.contentBytes())
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "shortDescription",
                                    CoreMatchers.`is`<String?>(documentUpload.shortDescription)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "longDescription",
                                    CoreMatchers.`is`<String?>(documentUpload.longDescription)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "title",
                                    CoreMatchers.`is`<String?>(documentUpload.title)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "mimetype",
                                    CoreMatchers.`is`<String?>(documentUpload.mimetype)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "extension",
                                    CoreMatchers.`is`<String?>(documentUpload.extension)
                                ),
                                FieldMatcher.Companion.hasField<Any, String>(
                                    "verificationReport",
                                    CoreMatchers.`is`<String>(verificationReport)
                                ),
                                FieldMatcher.Companion.hasField<Any, Any>("hash", CoreMatchers.notNullValue())
                            )
                        )
                    )
                }
        }
    }

    private fun assertDocumentInDb(
        sessionInfo: Helper.SessionInfo,
        documentUpload: DocumentUploadByLink,
        content: ByteArray,
        verificationReport: String
    ) {
        withTransaction(
            sessionFactory
        ) { session, tx ->
            session.find(
                Session::class.java, sessionInfo.id
            )
                .chain { found -> Mutiny.fetch(found.documents) }
                .invoke { docs ->
                    MatcherAssert.assertThat(
                        docs, CoreMatchers.hasItem<Any>(
                            CoreMatchers.allOf<Any>(
                                FieldMatcher.Companion.hasField<Any, UUID>(
                                    "id", CoreMatchers.`is`<UUID>(
                                        documentUpload.id!!.id
                                    )
                                ),
                                FieldMatcher.Companion.hasField<Any, ByteArray>(
                                    "content",
                                    CoreMatchers.`is`<ByteArray>(content)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "shortDescription",
                                    CoreMatchers.`is`<String?>(documentUpload.shortDescription)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "longDescription",
                                    CoreMatchers.`is`<String?>(documentUpload.longDescription)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "title",
                                    CoreMatchers.`is`<String?>(documentUpload.title)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "mimetype",
                                    CoreMatchers.`is`<String?>(documentUpload.mimetype)
                                ),
                                FieldMatcher.Companion.hasField<Any, String?>(
                                    "extension",
                                    CoreMatchers.`is`<String?>(documentUpload.extension)
                                ),
                                FieldMatcher.Companion.hasField<Any, String>(
                                    "verificationReport",
                                    CoreMatchers.`is`<String>(verificationReport)
                                ),
                                FieldMatcher.Companion.hasField<Any, Any>("hash", CoreMatchers.notNullValue())
                            )
                        )
                    )
                }
        }
    }

    class UploadDescription {
        var id: DocumentId? = null
        var content: Path? = null
        var title: String? = null
        var shortDescription: String? = null
        var longDescription: String? = null
        var mimetype: String? = null
        var extension: String? = null
        fun contentBytes(): ByteArray {
            return try {
                Files.readAllBytes(content)
            } catch (ex: IOException) {
                throw IllegalArgumentException("Could not read given content", ex)
            }
        }

        fun asSpec(): RequestSpecification {
            return RestAssured.given()
                .multiPart("content", content!!.toFile(), mimetype)
                .multiPart("id", id!!.id.toString())
                .multiPart("title", title)
                .multiPart("shortDescription", shortDescription)
                .multiPart("longDescription", longDescription)
                .contentType(ContentType.MULTIPART)
        }
    }

    fun textFile(baseTempDir: Path): UploadDescription {
        val extension = "txt"
        val inputUpload = baseTempDir.resolve("inputUpload.$extension")
        try {
            Files.write(inputUpload, genBytes())
        } catch (ex: IOException) {
            throw IllegalArgumentException("Could not initalize test data", ex)
        }
        val result = UploadDescription()
        result.id = DocumentId(UUID.randomUUID())
        result.content = inputUpload
        result.extension = extension
        result.mimetype = "text/plain"
        result.title = "Some title"
        result.shortDescription = "short"
        result.longDescription = "long"
        return result
    }

    companion object {
        private var downloadServer: WireMockServer? = null
        private var downloadServerBaseUrl: String? = null
        const val DOCUMENTS_PATH = "/api/v1/session/{sessionId}/submission/documents"
        const val START_TASK_PATH = "/api/v1/session/{sessionId}/task"
        @JvmStatic
        @BeforeAll
        fun init() {
            downloadServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            downloadServer!!.start()
            downloadServerBaseUrl = downloadServer!!.baseUrl()
        }

        @AfterAll
        fun cleanUp() {
            if (downloadServer != null) {
                downloadServer!!.stop()
            }
        }

        fun someDownloadableDocument(): DocumentBuilder {
            return DocumentBuilder(createDocument(), "here is content", false).isDownloadable()
        }

        fun someDocument(): DocumentBuilder {
            return DocumentBuilder(createDocument(), null, false)
        }

        private fun createDocument(): DocumentUploadByLink {
            val result = DocumentUploadByLink()
            result.id = DocumentId(UUID.randomUUID())
            result.location = URI.create(downloadServerBaseUrl + "/" + UUID.randomUUID().toString() + "/")
            result.title = genString(30)
            result.mimetype = genString(10)
            result.extension = genString(10)
            result.longDescription = genString(60)
            result.shortDescription = genString(10)
            return result
        }

        private fun someRawDocument(): String {
            return createRandomJsonData().toString()
        }

        private fun someVerificationReport(): String {
            return RandomStringUtils.randomAlphabetic(512)
        }

        private fun matchesInputStream(expected: ByteArray): InputStream {
            ArgumentMatchers.argThat { input: InputStream? -> matchesExpected(input, String(expected)) }
            return InputStream.nullInputStream()
        }

        private fun matchesInputStream(expected: String): InputStream {
            ArgumentMatchers.argThat { input: InputStream? -> matchesExpected(input, expected) }
            return InputStream.nullInputStream()
        }

        @Throws(RuntimeException::class)
        private fun matchesExpected(inputStream: InputStream?, expectedData: String?): Boolean {
            return try {
                if (inputStream == null) {
                    return expectedData == null
                }
                val actual = CharStreams.toString(InputStreamReader(inputStream))
                inputStream.reset()
                actual == expectedData
            } catch (ex: IOException) {
                throw RuntimeException("Could not match on the stream", ex)
            }
        }
    }
}
