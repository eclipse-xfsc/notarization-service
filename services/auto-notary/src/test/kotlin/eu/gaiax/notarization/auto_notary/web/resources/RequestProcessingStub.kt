package eu.gaiax.notarization.auto_notary.web.resources

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import eu.gaiax.notarization.auto_notary.application.AutoApprovingService
import eu.gaiax.notarization.auto_notary.web.client.*
import jakarta.ws.rs.core.MediaType
import java.time.OffsetDateTime
import java.util.*

/**
 *
 * @author Neil Crossley
 */
class RequestProcessingStub(val wireMock: WireMockServer) {
    private class Request(val id: String, val profileId: String)

    private val availableRequests: MutableList<Request> = ArrayList()
    private val errorAvailableRequests: MutableList<Request> = ArrayList()
    private val claimedRequests: List<Request> = ArrayList()
    private val errorClaimedRequests: List<Request> = ArrayList()
    private fun addCaimableAcceptableRequest(id: String, profileId: String) {
        val request = Request(id, profileId)
        availableRequests.add(request)
        val scenario = requestScenario(id)
        val path = requestPath(request)
        wireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(path + "claim"))
                .inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .withHeader(
                    "Authorization",
                    WireMock.equalTo("Bearer " + MockAuthServerResource.Companion.ACCESS_TOKEN_1)
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(201)
                ).atPriority(100)
                .willSetStateTo(stateClaimed)
        )
        wireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(path + "accept"))
                .inScenario(scenario)
                .whenScenarioStateIs(stateClaimed)
                .withHeader(
                    "Authorization",
                    WireMock.equalTo("Bearer " + MockAuthServerResource.Companion.ACCESS_TOKEN_1)
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(201)
                ).atPriority(100)
                .willSetStateTo("done")
        )
    }

    private fun requestScenario(id: String): String {
        return "scenario $id"
    }

    private fun requestPath(request: Request): String {
        return "/api/v1/profiles/" + request.profileId + "/requests/" + request.id + "/"
    }

    fun addCaimableRequest() {
        addCaimableAcceptableRequest("id" + UUID.randomUUID(), "profile" + UUID.randomUUID())
    }

    fun addFailingCaimableRequest() {
        this.addFailingCaimableRequest("id" + UUID.randomUUID(), "profile" + UUID.randomUUID(), 400)
    }

    private fun addFailingCaimableRequest(id: String, profileId: String, status: Int) {
        val request = Request(id, profileId)
        errorAvailableRequests.add(request)
        val path = requestPath(request)
        wireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(path + "claim"))
                .withHeader(
                    "Authorization",
                    WireMock.equalTo("Bearer " + MockAuthServerResource.Companion.ACCESS_TOKEN_1)
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(status)
                ).atPriority(100)
        )
    }

    fun registerRequests() {

        registerPagingRequests(availableRequests, errorAvailableRequests, "Paging, Claiming, Accepting", "available")
        registerPagingRequests(claimedRequests, errorClaimedRequests, "Paging, Accepting", "ownClaimed")
    }

    @Throws(IllegalArgumentException::class)
    private fun registerPagingRequests(
        requests: List<Request>,
        errors: List<Request>,
        requestsScenario: String,
        filterType: String
    ) {
        val firstPageProcessed = "firstPageProcessed$filterType"
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/requests"))
                .inScenario(requestsScenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .withQueryParam("offset", WireMock.equalTo("0"))
                .withQueryParam("limit", WireMock.equalTo(Integer.toString(AutoApprovingService.PAGE_SIZE)))
                .withQueryParam("filter", WireMock.equalTo(filterType))
                .withHeader(
                    "Authorization",
                    WireMock.equalTo("Bearer " + MockAuthServerResource.Companion.ACCESS_TOKEN_1)
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withJsonBody(asJsonSummary(requests, errors))
                ).atPriority(100)
                .willSetStateTo(firstPageProcessed)
        )
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/requests"))
                .inScenario(requestsScenario)
                .whenScenarioStateIs(firstPageProcessed)
                .withQueryParam("offset", WireMock.equalTo("0"))
                .withQueryParam("limit", WireMock.equalTo(Integer.toString(AutoApprovingService.PAGE_SIZE)))
                .withQueryParam("filter", WireMock.equalTo(filterType))
                .withHeader(
                    "Authorization",
                    WireMock.equalTo("Bearer " + MockAuthServerResource.Companion.ACCESS_TOKEN_1)
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withJsonBody(asJsonSummary(errors))
                ).atPriority(100)
        )
    }

    private fun asJsonSummary(vararg allRequests: List<Request>): JsonNode {
        return objectMapper.valueToTree(asSummary(*allRequests))
    }

    private fun asSummary(vararg allRequests: List<Request>): RequestProcessingRestClient.PagedNotarizationRequestSummary {
        val results: MutableList<RequestProcessingRestClient.NotarizationRequestSummary> =
            ArrayList()
        for (requests in allRequests) {
            for (request in requests) {
                results.add(
                    RequestProcessingRestClient.NotarizationRequestSummary(
                        request.id,
                        request.profileId,
                        OffsetDateTime.now().minusHours(2),
                        OffsetDateTime.now().minusHours(1),
                        "readyForReview",
                        "did:something:or-other",
                        0,
                        null
                    )
                )
            }
        }
        return RequestProcessingRestClient.PagedNotarizationRequestSummary(
            1, results.size.toLong(), results
        )
    }

    companion object {
        private const val stateClaimed = "claimed"
        private const val stateAccepted = "accepted"
        private val objectMapper = ObjectMapper()

        init {
            objectMapper.registerModule(JavaTimeModule())
        }
    }
}
