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

import com.fasterxml.jackson.databind.JsonNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.http.*
import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import eu.gaiax.notarization.api.profile.NotaryAccess
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.DataGen
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.domain.entity.RequestorIdentity
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.extensions.ExternalServicesWireMock
import eu.gaiax.notarization.request_processing.extensions.MockExternalServicesResource
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.SessionTaskSummary
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hibernate.reactive.mutiny.Mutiny
import org.jose4j.jwe.JsonWebEncryption
import org.jose4j.lang.JoseException
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@QuarkusTestResource(MockExternalServicesResource::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
class IdentityResourceTest {
    @ExternalServicesWireMock
    lateinit var wireMockServer: WireMockServer

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @ConfigProperty(name = "quarkus.http.port")
    var assignedPort: Int = 0

    fun configMock() {

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
    @Throws(MalformedURLException::class, JoseException::class)
    fun identificationCanBeCancelled() {
        configMock()
        val someSession: Helper.SessionInfo =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.someProfileId).session
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", someSession.accessToken)
            .`when`().get(someSession.location)
            .then()
            .statusCode(200)
            .extract()
            .path<Any>("tasks")
        val tasklist = listOf(
            *MockState.mapper.convertValue(
                tasks,
                Array<SessionTaskSummary>::class.java
            )
        )
        val task = tasklist.first { t -> t.name?.contains("identify") ?: false }

        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(SESSION_VARIABLE, someSession.id)
            .header("token", someSession.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)

        //cancel
        RestAssured.given()
            .pathParam(SESSION_VARIABLE, someSession.id)
            .header("token", someSession.accessToken)
            .param("taskId", task.taskId.toString())
            .`when`()
            .delete(TASK_PATH)
            .then()
            .statusCode(204)
        MatcherAssert.assertThat<Boolean>(
            wireMockServer.getAllServeEvents().stream()
                .filter { evt ->
                    evt.getRequest().getMethod().isOneOf(RequestMethod.DELETE)
                }.findAny().isPresent(),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @Throws(MalformedURLException::class, JoseException::class)
    fun identificationGetsCancelledIfNewOneIsStarted() {
        configMock()
        val someSession: Helper.SessionInfo =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.someProfileId).session
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", someSession.accessToken)
            .`when`().get(someSession.location)
            .then()
            .statusCode(200)
            .extract()
            .path<Any>("tasks")
        val tasklist = Arrays.asList<SessionTaskSummary>(
            *MockState.Companion.mapper.convertValue<Array<SessionTaskSummary>>(
                tasks,
                Array<SessionTaskSummary>::class.java
            )
        )
        val task = tasklist.stream()
            .filter { t -> t.name?.contains("identify") ?: false }
            .findAny().orElseThrow()

        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(SESSION_VARIABLE, someSession.id)
            .header("token", someSession.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)

        //startTask again
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(SESSION_VARIABLE, someSession.id)
            .header("token", someSession.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)
        MatcherAssert.assertThat<Boolean>(
            wireMockServer.getAllServeEvents().stream()
                .filter { evt ->
                    evt.getRequest().getMethod().isOneOf(RequestMethod.DELETE)
                }.findAny().isPresent(),
            CoreMatchers.`is`<Boolean>(true)
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00014")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @Throws(MalformedURLException::class, JoseException::class, URISyntaxException::class)
    fun identificationIsStoredAfterIdentification() {
        val inputIdentity: JsonNode = DataGen.createRandomJsonData()
        val inputProfile: Profile = MockState.someProfile
        configMock()
        val someSession: Helper.SessionInfo =
            Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory, MockState.someProfileId).session
        val tasks: Any = RestAssured.given()
            .accept(ContentType.JSON)
            .header("token", someSession.accessToken)
            .`when`().get(someSession.location)
            .then()
            .statusCode(200)
            .extract()
            .path<Any>("tasks")
        val tasklist = Arrays.asList(
            *MockState.mapper.convertValue(
                tasks,
                Array<SessionTaskSummary>::class.java
            )
        )
        val task = tasklist.first { t -> t.name?.contains("identify") ?: false }

        //startTask
        RestAssured.given()
            .contentType(ContentType.JSON)
            .pathParam(SESSION_VARIABLE, someSession.id)
            .header("token", someSession.accessToken)
            .queryParam("taskId", task.taskId)
            .`when`()
            .post(TASK_PATH)
            .then()
            .statusCode(201)

        //hijack success url
        val successUri: String = wireMockServer.allServeEvents
            .map { it.request.queryParams }
            .map { it["success"] }
            .first { it?.isPresent ?: false }
            .let { it!!.firstValue() }
        //reencode nonce?
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(inputIdentity)
            .`when`()
            .post(asCallbackUrl(successUri))
            .then()
            .statusCode(204)
        val res = Helper.withTransaction<Session>(
            sessionFactory
        ) { session, tx ->
            session.find(
                Session::class.java, someSession.id
            )
                .call { foundSession: Session ->
                    Mutiny.fetch(
                        foundSession.identities
                    )
                }
        }
        MatcherAssert.assertThat(
            "expected nbr of identities in session does not match",
            res.identities,
            hasSize(inputProfile.notaries.size)
        )
        val requestorIdentity: RequestorIdentity = res.identities!!.iterator().next()
        val notaryAccess = inputProfile.notaries.iterator().next()
        assertThat(requestorIdentity.algorithm, equalTo(notaryAccess.algorithm))
        assertThat(requestorIdentity.encryption, equalTo(inputProfile.encryption))
        val ecnryptedReqData = requestorIdentity.data
        val plain = decryptedIdentity(notaryAccess, ecnryptedReqData)
        MatcherAssert.assertThat(plain, CoreMatchers.`is`(inputIdentity.toString()))
    }

    @Throws(JoseException::class)
    private fun decryptedIdentity(notary: NotaryAccess, payload: String?): String {
        val jwe = JsonWebEncryption()
        val priv = notary.key.getPrivateKey()
        jwe.setKey(priv)
        jwe.compactSerialization = payload
        return jwe.payload
    }

    fun asCallbackUrl(rawUrl: String?): URL {
        return try {
            UriBuilder.fromUri(rawUrl).port(assignedPort).build().toURL()
        } catch (ex: MalformedURLException) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        private const val SESSION_VARIABLE = "sessionId"
        const val TASK_PATH = "/api/v1/session/{sessionId}/task"
    }
}
