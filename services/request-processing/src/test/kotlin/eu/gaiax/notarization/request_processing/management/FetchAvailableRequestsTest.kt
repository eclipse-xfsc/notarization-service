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
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransaction
import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.DocumentFull
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.DocumentFull.Companion.fromDocument
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.DocumentView
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState
import eu.gaiax.notarization.request_processing.infrastructure.rest.mock.MockState.Notary
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.util.*
import java.util.Set
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
class FetchAvailableRequestsTest {
    var objectMapper = ObjectMapper()

    @Inject
    lateinit var sessionFactory: Mutiny.SessionFactory
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun invalidLimitPaginateFetchAvailableRequests() {
        val filterVal = RequestFilter.available
        val limit = 0
        pruneDB()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .queryParam("offset", 0)
            .queryParam("limit", limit)
            .queryParam("filter", filterVal)
            .`when`()[REQUESTS_PATH]
            .then()
            .statusCode(400)
    }

    class TotalRec_Limit(val totalRequests: Int, val limitPerPage: Int)

    private fun createClaimedRequest(profile: ProfileId, notary: String): NotarizationRequest {
        val nr = NotarizationRequest()
        val id = UUID.randomUUID()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = UUID.randomUUID().toString()
            sess.state = NotarizationRequestState.WORK_IN_PROGRESS
            sess.profileId = ProfileId(profile.id)
            nr.id = id
            nr.session = sess
            nr.claimedBy = notary
            session.persist(sess).chain{ _ -> session.persist(nr) }
        }.await().indefinitely()
        return nr
    }

    private fun pruneDB() {
        withTransactionAsync(sessionFactory) { session, tx ->
            session.createMutationQuery("delete Document").executeUpdate().chain(
                Supplier { session.createMutationQuery("delete NotarizationRequest").executeUpdate() })
        }
            .await().indefinitely()
    }

    private fun createRequestWithStateInDB(state: NotarizationRequestState, profile: ProfileId): NotarizationRequest {
        val nr = NotarizationRequest()
        val id = UUID.randomUUID()
        withTransactionAsync<Void>(sessionFactory) { session, tx ->
            val sess = Session()
            sess.id = UUID.randomUUID().toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            nr.id = id
            nr.session = sess
            session.persist(sess).chain { _ -> session.persist(nr) }
        }.await().indefinitely()
        return nr
    }

    class ExpectedResultsByFilter(
        val available: Array<String>,
        val claimed: Array<String>,
        val ownClaimed: Array<String>
    )

    private val randomNotary: Notary
        private get() = getRandomNotary(null)

    private fun getRandomNotary(except: Notary?): Notary {
        return if (except != null) {
            MockState.notariesWithSomeProfile.stream()
                .filter { n: Notary -> !n.name.equals(except.name) }
                .skip(random.nextInt(0, MockState.notariesWithSomeProfile.size - 1).toLong())
                .findFirst().orElseThrow()
        } else {
            MockState.notariesWithSomeProfile.stream()
                .skip(random.nextInt(0, MockState.notariesWithSomeProfile.size).toLong())
                .findFirst().orElseThrow()
        }
    }

    class Totals(var totalClaimedBy: Int, var totalClaimedByOther: Int, var totalAvailable: Int)

    private fun prepareFilterTest(claimingNotary: Notary, totals: Totals): ExpectedResultsByFilter {
        pruneDB()
        val profile = MockState.someProfileId
        val claimedByNotary = Stream.generate { createClaimedRequest(profile, claimingNotary.name) }
            .limit(totals.totalClaimedBy.toLong())
            .map { r: NotarizationRequest -> r.id.toString() }
            .collect(Collectors.toList())
        val allClaimed = Stream.generate { createClaimedRequest(profile, getRandomNotary(claimingNotary).name) }
            .limit(totals.totalClaimedByOther.toLong())
            .map { r: NotarizationRequest -> r.id.toString() }
            .collect(Collectors.toList())
        allClaimed.addAll(claimedByNotary)
        val available =
            Stream.generate { createRequestWithStateInDB(NotarizationRequestState.READY_FOR_REVIEW, profile) }
                .limit(totals.totalAvailable.toLong())
                .map { r: NotarizationRequest -> r.id.toString() }
                .collect(Collectors.toList())
        return ExpectedResultsByFilter(
            available.toTypedArray(),
            allClaimed.toTypedArray(),
            claimedByNotary.toTypedArray()
        )
    }

    private fun givenFiltered(filterVal: RequestFilter, claimingNotary: Notary): ValidatableResponse {
        return RestAssured.given()
            .header("Authorization", claimingNotary.bearerValue())
            .queryParam("offset", 0)
            .queryParam("limit", Int.MAX_VALUE)
            .queryParam("filter", filterVal)
            .`when`()[REQUESTS_PATH]
            .then()
            .statusCode(200)
    }

    @ParameterizedTest
    @MethodSource("totals")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun testFilterAllClaimed(totals: Totals) {
        val claimingNotary = randomNotary
        val expectedNotarizationRequests = prepareFilterTest(claimingNotary, totals)
        givenFiltered(RequestFilter.allClaimed, claimingNotary)
            .body("notarizationRequests.id", Matchers.containsInAnyOrder(*expectedNotarizationRequests.claimed))
    }

    @ParameterizedTest
    @MethodSource("totals")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun testFilterOwnClaimed(totals: Totals) {
        val claimingNotary = randomNotary
        val expectedNotarizationRequests = prepareFilterTest(claimingNotary, totals)
        givenFiltered(RequestFilter.ownClaimed, claimingNotary)
            .body(
                "notarizationRequests.id",
                Matchers.containsInAnyOrder(*expectedNotarizationRequests.ownClaimed)
            )
    }

    @ParameterizedTest
    @MethodSource("totals")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun testFilterAvailable(totals: Totals) {
        val claimingNotary = randomNotary
        val expectedNotarizationRequests = prepareFilterTest(claimingNotary, totals)
        givenFiltered(RequestFilter.available, claimingNotary)
            .body(
                "notarizationRequests.id",
                Matchers.containsInAnyOrder(*expectedNotarizationRequests.available)
            )
    }

    @ParameterizedTest
    @MethodSource("testLimitValues")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun canPaginateFetchAvailableRequests(testValues: TotalRec_Limit) {
        val profile = MockState.someProfileId
        val nbrRequests = testValues.totalRequests
        val limit = testValues.limitPerPage
        val expectedPageCount = Math.ceil(nbrRequests.toDouble() / limit).toInt()

        //last page
        val offset = expectedPageCount - 1
        val expectedRequestsOnPage = nbrRequests - offset * limit
        pruneDB()
        for (i in 0 until nbrRequests) {
            createRequestWithStateInDB(NotarizationRequestState.READY_FOR_REVIEW, profile)
        }
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .queryParam("offset", offset)
            .queryParam("limit", limit)
            .`when`()[REQUESTS_PATH]
            .then()
            .statusCode(200)
            .body("requestCount", CoreMatchers.`is`(nbrRequests))
            .body("pageCount", CoreMatchers.`is`(expectedPageCount))
            .body("notarizationRequests.size()", CoreMatchers.`is`(expectedRequestsOnPage))
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun canFetchNotarizationRequestSummaryById(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(state, profile).id.toString()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", notRequestId)
            .pathParam("profileId", profile.id)
            .`when`()[REQUEST_PATH]
            .then()
            .body("id", CoreMatchers.`is`(notRequestId))
            .statusCode(200)
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun canNotFetchNotarizationRequestSummaryById(state: NotarizationRequestState) {
        val profile = MockState.someProfileId
        val notRequestId = createRequestWithStateInDB(state, profile).id
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", notRequestId)
            .pathParam("profileId", profile.id)
            .`when`()[REQUEST_PATH]
            .then()
            .statusCode(400)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun canNotFetchNotarizationRequestSummaryByUnknownId() {
        val profile = MockState.someProfileId
        createRequestWithStateInDB(NotarizationRequestState.WORK_IN_PROGRESS, profile)
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", UUID.randomUUID().toString())
            .pathParam("profileId", profile.id)
            .`when`()[REQUEST_PATH]
            .then()
            .statusCode(404)
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00028")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00008")
    fun canFetchNotarizationRequestDocumentById(state: NotarizationRequestState?) {
//    public void canFetchNotarizationRequestDocumentById(){
        val profile = MockState.someProfileId
        val docNRequest = createRequestWithStateAndDocumentInDB(NotarizationRequestState.READY_FOR_REVIEW, profile)
        val notRequestId = docNRequest.notReq.id.toString()
        val docSums = RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", notRequestId)
            .pathParam("profileId", profile.id)
            .`when`()[REQUEST_PATH]
            .then()
            .body("id", CoreMatchers.`is`(notRequestId))
            .statusCode(200)
            .extract().body().path<Any>("documents")
        val docViews = listOf(*objectMapper.convertValue(docSums, Array<DocumentView>::class.java))
        val docId = docViews.stream()
            .map { v: DocumentView -> v.id!!.id }
            .findAny().orElseThrow()
        RestAssured.given()
            .header("Authorization", MockState.notary1.bearerValue())
            .pathParam("notarizationRequestId", notRequestId)
            .pathParam("profileId", profile.id)
            .pathParam("documentId", docId)
            .`when`()[DOC_REQUEST_PATH]
            .then()
            .statusCode(200)
            .body("content", CoreMatchers.`is`(Base64.getEncoder().encodeToString(docNRequest.doc.content)))
            .body("verificationReport", CoreMatchers.`is`<String>(docNRequest.doc.verificationReport?.value))
    }

    private fun aDocument(sess: Session): Document {
        val doc = Document()
        doc.id = UUID.randomUUID()
        doc.content = "conntent".toByteArray()
        doc.title = "title"
        doc.shortDescription = "sdesc"
        doc.longDescription = "ldesc"
        doc.mimetype = "mime"
        doc.extension = "ext"
        doc.verificationReport = "<report>evaluation</report>"
        doc.hash = "hash"
        doc.session = sess
        return doc
    }

    class DocNRequest(
        val doc: DocumentFull,
        val notReq: NotarizationRequest
    )

    private fun createRequestWithStateAndDocumentInDB(
        state: NotarizationRequestState,
        profile: ProfileId
    ): DocNRequest {
        return withTransaction(sessionFactory) { session, tx ->
            val sess = Session()
            val nr = NotarizationRequest()
            val doc = aDocument(sess)
            val id = UUID.randomUUID()
            sess.id = UUID.randomUUID().toString()
            sess.state = state
            sess.profileId = ProfileId(profile.id)
            sess.documents = Set.of(doc)
            nr.id = id
            nr.session = sess
            doc.session = sess
            Uni.createFrom().voidItem()
                .chain(Supplier { session.persist(sess) })
                .chain(Supplier { session.persist(doc) })
                .chain(Supplier { session.persist(nr) })
                .map { d: Void? -> DocNRequest(fromDocument(doc), nr) }
        }
    }

    companion object {
        const val REQUESTS_PATH = "/api/v1/requests"
        const val REQUEST_PATH = "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}"
        const val DOC_REQUEST_PATH =
            "/api/v1/profiles/{profileId}/requests/{notarizationRequestId}/document/{documentId}"
        val random = Random()

        @JvmStatic
        fun allowedStates(): Stream<NotarizationRequestState> {
            return Stream.of(
                NotarizationRequestState.READY_FOR_REVIEW,
                NotarizationRequestState.WORK_IN_PROGRESS,
                NotarizationRequestState.PRE_ACCEPTED,
                NotarizationRequestState.ACCEPTED,
                NotarizationRequestState.PENDING_DID
            )
        }

        @JvmStatic
        fun notAllowedStates(): Stream<NotarizationRequestState> {
            val updateable = allowedStates().collect(Collectors.toSet())
            return Stream.of(*NotarizationRequestState.values())
                .filter { s: NotarizationRequestState -> !updateable.contains(s) }
        }

        @JvmStatic
        fun testLimitValues(): Stream<TotalRec_Limit> {
            return Stream.of(
                TotalRec_Limit(101, 1),
                TotalRec_Limit(101, 100),
                TotalRec_Limit(101, 101),
                TotalRec_Limit(101, 102),
                TotalRec_Limit(101, 500)
            )
        }

        @JvmStatic
        private fun totals(): Stream<Totals> {
            return Stream.of(
                Totals(0, 0, 0),
                Totals(0, 0, 1),
                Totals(0, 1, 0),
                Totals(0, 1, 1),
                Totals(1, 0, 0),
                Totals(1, 0, 1),
                Totals(1, 1, 0),
                Totals(1, 1, 1),
                Totals(1, 1, 10),
                Totals(0, 1, 10),
                Totals(11, 13, 0),
                Totals(11, 13, 23),
                Totals(random.nextInt(0, 10), random.nextInt(0, 10), random.nextInt(0, 10)),
                Totals(random.nextInt(0, 10), random.nextInt(0, 10), random.nextInt(0, 10)),
                Totals(random.nextInt(0, 10), random.nextInt(0, 10), random.nextInt(0, 10)),
                Totals(random.nextInt(0, 10), random.nextInt(0, 10), random.nextInt(0, 10))
            )
        }
    }
}
