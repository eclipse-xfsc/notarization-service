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

import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.request_processing.DataGen.genString
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditLogFetchIdentity
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.auditTrailForNotarizationRequestID
import eu.gaiax.notarization.request_processing.matcher.AuditTrailMatcher.hasAuditEntries
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.time.Duration
import java.util.*
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Supplier
import java.util.stream.Collectors
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
class FetchIdentityTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory

    class NotReqId_Identies(
        val notReqId: String,
        val identities: Set<RequestorIdentity>
    )

    private fun createRequestWithStateInDB(
        state: NotarizationRequestState,
        profile: ProfileId,
        nbrIdentities: Int
    ): NotReqId_Identies {
        val id = UUID.randomUUID()
        val setOfIdentities = HashSet<RequestorIdentity>()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            var chain = Uni.createFrom().nullItem<Any?>()
            val sess = Session()
            sess.id = UUID.randomUUID().toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            sess.tasks = HashSet()
            val nr = NotarizationRequest()
            nr.id = id
            nr.session = sess
            sess.identities = HashSet()
            for (i in 0 until nbrIdentities) {
                val reqId = RequestorIdentity()
                reqId.id = UUID.randomUUID()
                reqId.algorithm = "An alogrithm"
                reqId.data = genString()
                reqId.encryption = "encryption"
                reqId.jwk = "jwk"
                reqId.session = sess
                setOfIdentities.add(reqId)
                sess.identities!!.add(reqId)
                chain = chain.chain(Supplier { session.persist(reqId) })
            }
            chain
                .chain( { _ -> session.persist(sess) })
                .chain({ _ -> session.persist(nr) })
        }.await().atMost(Duration.ofSeconds(20))
        return NotReqId_Identies(id.toString(), setOfIdentities)
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canFetchIdentity(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids_identities = createRequestWithStateInDB(state, profile, random.nextInt(1, 10))
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", ids_identities.notReqId)
            .`when`()[REQUEST_PATH]
            .then()
            .statusCode(200)
            .body(
                "[0].data", Matchers.oneOf<String>(
                    *ids_identities.identities
                        .map { i: RequestorIdentity -> i.data }
                        .toTypedArray()
                )
            )
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(
                ids_identities.notReqId,
                NotarizationRequestAction.FETCH_IDENTITY,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogFetchIdentity())
        )
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotFetchIdentity(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val ids_identities = createRequestWithStateInDB(state, profile, random.nextInt(1, 10))
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", ids_identities.notReqId)
            .`when`()[REQUEST_PATH]
            .then()
            .statusCode(400)
        MatcherAssert.assertThat<List<HttpNotarizationRequestAudit>>(
            auditTrailForNotarizationRequestID(
                ids_identities.notReqId,
                NotarizationRequestAction.FETCH_IDENTITY,
                1,
                sessionFactory
            ),
            hasAuditEntries(auditLogFetchIdentity().httpStatus(400))
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00039")
    fun canNotFetchIdentity() {
        val profile = MockState.someProfileId
        createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, profile, random.nextInt(1, 10))
        val unkownId = UUID.randomUUID().toString()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("profileId", profile.id)
            .pathParam("notarizationRequestId", unkownId)
            .`when`()[REQUEST_PATH]
            .then()
            .statusCode(404)
        MatcherAssert.assertThat(
            auditTrailForNotarizationRequestID(unkownId, NotarizationRequestAction.FETCH_IDENTITY, 1, sessionFactory),
            hasAuditEntries(auditLogFetchIdentity().httpStatus(404))
        )
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/identity"
        val random = Random()

        @JvmStatic
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.WORK_IN_PROGRESS,
                NotarizationRequestState.PRE_ACCEPTED,
            )
        }

        @JvmStatic
        fun notAllowedStates(): Stream<NotarizationRequestState> {
            val updateable = allowedStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !updateable.contains(s) }
        }
    }
}
