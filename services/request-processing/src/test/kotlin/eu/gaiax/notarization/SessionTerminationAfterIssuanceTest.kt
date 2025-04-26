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
package eu.gaiax.notarization

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.MockSsiIssuanceLifecycleManager
import eu.gaiax.notarization.request_processing.Helper
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.entity.RequestorIdentity
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.net.MalformedURLException
import java.util.*
import java.util.function.Supplier

/**
 *
 * @author Florian Otto
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
@QuarkusTestResource(
    RabbitMqTestResourceLifecycleManager::class
)
@QuarkusTestResource(MockSsiIssuanceLifecycleManager::class)
class SessionTerminationAfterIssuanceTest {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    @SsiIssuanceWireMock
    var issuanceWireMock: WireMockServer? = null

    @ConfigProperty(name = "quarkus.http.port")
    var assignedPort: Int = 0

    class NotAndSessId(
        val notReqId: String,
        val sessId: String,
    )

    private fun createRequestWithStateInDB(
        state: NotarizationRequestState,
        didSet: Boolean,
        profile: ProfileId
    ): NotAndSessId {
        val notReqId = UUID.randomUUID()
        val sessId = UUID.randomUUID()
        Helper.withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = sessId.toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            val nr = NotarizationRequest()
            nr.id = notReqId
            nr.session = sess
            if (didSet) {
                nr.did = "did.action:value"
                nr.requestorInvitationUrl = "some-invitation"
            }
            session.persist(sess)
                .chain { _ -> session.persist(nr) }
        }.await().indefinitely()
        return NotAndSessId(
            notReqId.toString(),
            sessId.toString()
        )
    }

    private fun assertStateInDB(id: String, state: NotarizationRequestState) {
        Helper.withTransactionAsync(sessionFactory) { dbSession: Mutiny.Session, tx: Mutiny.Transaction? ->
            dbSession.find(
                Session::class.java, id
            ).invoke { session: Session -> MatcherAssert.assertThat(session.state, Matchers.`is`(state)) }
        }.await().indefinitely()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00013")
    @Throws(MalformedURLException::class)
    fun successCB_finishesSessionToIssued() {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, true, profile)
        addIdentities(ids.sessId)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        issuanceWireMock!!.verify(
            WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "$.successURL",
                        WireMock.containing("finishNotarizationRequest")
                    )
                )
        )
        var successUri = issuanceWireMock!!.allServeEvents.stream()
            .map { e: ServeEvent -> e.request.bodyAsString }
            .filter { s: String? -> s != null && !s.isEmpty() }
            .map { str: String? ->
                try {
                    val js = objectMapper!!.readTree(str)
                    return@map js["successURL"].asText()
                } catch (ex: JsonProcessingException) {
                    throw RuntimeException(ex)
                }
            }
            .findFirst().orElseThrow()
        successUri = fixUrl(successUri)
        RestAssured.given().`when`()
            .post(successUri)
            .then()
        MatcherAssert.assertThat<UUID>(UUID.fromString(ids.sessId), sessionCleaned)
        assertStateInDB(ids.sessId, NotarizationRequestState.ISSUED)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            AuditTrailMatcher.auditTrailFor(
                ids.sessId,
                NotarizationRequestAction.ISSUANCE_FINISH_SUCCESS,
                sessionFactory
            ),
            AuditTrailMatcher.hasAuditEntries(AuditTrailMatcher.auditLogFinSuccess())
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00013")
    @Throws(MalformedURLException::class)
    fun failCB_finishesSessionToTerminated() {
        val profile = MockState.someProfileId
        val ids = createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, true, profile)
        addIdentities(ids.sessId)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", ids.notReqId)
            .pathParam("profileId", profile.id)
            .`when`()
            .post(REQUEST_PATH)
            .then()
            .statusCode(204)
        issuanceWireMock!!.verify(
            WireMock.postRequestedFor(WireMock.urlMatching("/credential/start-issuance/"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "$.successURL",
                        WireMock.containing("finishNotarizationRequest")
                    )
                )
        )
        var failureURL = issuanceWireMock!!.allServeEvents.stream()
            .map { e: ServeEvent -> e.request.bodyAsString }
            .filter { s: String? -> s != null && !s.isEmpty() }
            .map { str: String? ->
                try {
                    val js = objectMapper!!.readTree(str)
                    return@map js["failureURL"].asText()
                } catch (ex: JsonProcessingException) {
                    throw RuntimeException(ex)
                }
            }
            .findFirst().orElseThrow()
        failureURL = fixUrl(failureURL)
        RestAssured.given().`when`()
            .post(failureURL)
            .then()
        assertStateInDB(ids.sessId, NotarizationRequestState.TERMINATED)
        MatcherAssert.assertThat<UUID>(UUID.fromString(ids.sessId), sessionCleaned)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            AuditTrailMatcher.auditTrailFor(
                ids.sessId,
                NotarizationRequestAction.ISSUANCE_FINISH_FAIL,
                sessionFactory
            ),
            AuditTrailMatcher.hasAuditEntries(AuditTrailMatcher.auditLogFinFail())
        )
    }

    @Throws(MalformedURLException::class)
    private fun fixUrl(url: String): String {
        return UriBuilder.fromUri(url).port(assignedPort!!).build().toURL().toString()
    }

    private fun addIdentities(sessionId: String): UUID {
        val identiyId = UUID.randomUUID()
        Helper.withTransaction<Session>(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
            val q = session.createSelectionQuery(
                "from Session s left join fetch s.identities where s.id = :id",
                Session::class.java
            )
            q.setParameter("id", sessionId)
            q.singleResult
                .call { s: Session ->
                    val identiy = RequestorIdentity()
                    identiy.id = identiyId
                    identiy.session = s
                    s.identities!!.add(identiy)
                    session.persist(s)
                }
        }
        return identiyId
    }

    var sessionCleaned: BaseMatcher<UUID> = object : BaseMatcher<UUID>() {
        private var reason = ""
        override fun matches(id: Any): Boolean {
            reason = ""
            val s = Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                session.find(
                    Session::class.java, id.toString()
                )
            }
            //we also check if identities are deleted just to be sure
            val identities_count =
                Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                    val q =
                        session.createNativeQuery<Long>("select count(id) from requestor_identity r where r.session_id = :sid")
                    q.setParameter("sid", id.toString())
                    q.singleResult
                }
            val documents_count =
                Helper.withTransaction(sessionFactory) { session: Mutiny.Session, tx: Mutiny.Transaction? ->
                    val q =
                        session.createNativeQuery<Long>(" select count(id) from document d where d.session_id = :sid")
                    q.setParameter("sid", id.toString())
                    q.singleResult
                }
            return if (s != null && (s.state === NotarizationRequestState.TERMINATED || s.state === NotarizationRequestState.ISSUED)) {
                if (identities_count.toInt() != 0) {
                    reason += "found identity, count was: $identities_count"
                    return false
                }
                if (documents_count.toInt() != 0) {
                    reason += "found documents with no association, count was: $documents_count"
                    return false
                }
                true
            } else {
                reason += if (s == null) {
                    "found no session"
                } else {
                    "found session with state: '" + s.state + "' instead of TERMINATED or ISSUED"
                }
                false
            }
        }

        override fun describeTo(description: Description) {
            description.appendText("a session with terminated state in database")
        }

        override fun describeMismatch(item: Any, description: Description) {
            description.appendText("<$item> was errornous, reason is: $reason")
        }
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/accept"
    }
}
