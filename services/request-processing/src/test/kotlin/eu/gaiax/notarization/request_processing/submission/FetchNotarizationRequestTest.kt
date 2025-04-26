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
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogFetch
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailFor
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
class FetchNotarizationRequestTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @Test
    @Throws(JsonProcessingException::class)
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    fun canFetchSubmittedNotarizationRequestByLocation() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        val res: Response = RestAssured.given().accept(ContentType.JSON)
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken)
            .`when`()[Helper.SUBMISSION_PATH]
            .then()
            .statusCode(200)
            .body("holder", CoreMatchers.`is`<Any>(sessionWithNotarizationRequest.notarizationRequest.holder?.value))
            .body("profileId", `is`(sessionWithNotarizationRequest.session.profileId))
            .extract().response()
        val returnedData = objectMapper.readTree(res.body().asString())
        assertThat(
            returnedData["data"].toPrettyString(),
            `is`(sessionWithNotarizationRequest.notarizationRequest.data?.toPrettyString())
        )
        assertThat(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.FETCH,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogFetch())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    fun invalidTokenDetectedOnFetchNotRequest() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        RestAssured.given().accept(ContentType.JSON)
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", sessionWithNotarizationRequest.session.accessToken + "INVALID")
            .`when`()[Helper.SUBMISSION_PATH]
            .then()
            .statusCode(401)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.FETCH,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogFetch().httpStatus(401))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    fun noTokenDetectedOnFetchNotRequest() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        RestAssured.given().accept(ContentType.JSON)
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .`when`()[Helper.SUBMISSION_PATH]
            .then()
            .statusCode(400)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.FETCH,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogFetch().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    fun emptyTokenDetectedOnFetchNotRequest() {
        val sessionWithNotarizationRequest = Helper.prepareSessionWithSubmittedNotarizationRequest(sessionFactory)
        RestAssured.given().accept(ContentType.JSON)
            .pathParam(
                Helper.SESSION_VARIABLE,
                sessionWithNotarizationRequest.session.id
            )
            .header("token", "")
            .`when`()[Helper.SUBMISSION_PATH]
            .then()
            .statusCode(401)
        MatcherAssert.assertThat(
            auditTrailFor(
                sessionWithNotarizationRequest.session.id,
                NotarizationRequestAction.FETCH,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogFetch().httpStatus(401))
        )
    }

    companion object {
        const val DOCUMENTS_PATH = "/api/v1/session/{sessionId}/submission/documents"
    }
}
