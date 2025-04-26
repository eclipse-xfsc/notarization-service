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
 ***************************************************************************/

package eu.gaiax.notarization

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import eu.xfsc.not.vc.status.StatusBitSet
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.junit.QuarkusTest
import io.restassured.matcher.ResponseAwareMatcher
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.util.*
import kotlin.streams.asStream


@QuarkusTest
@QuarkusTestResource(IssueServiceMock::class)
class IssueTest {

    lateinit var profile: String
    lateinit var listName: String

    @BeforeEach
    fun registerProfile() {
        profile = UUID.randomUUID().toString()

        listName =
            Given {
                queryParam("profile", profile)
                queryParam("issue-list-credential", false)
            } When {
                post("/management/lists")
            } Then {
                statusCode(200)
            } Extract {
                asString()
            }

    }


    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00024")
    fun duplicateCreationOfProfileReturnsConflict() {
        profile = UUID.randomUUID().toString()

        Given {
            queryParam("profile", profile)
        } When {
            post("/management/lists")
        } Then {
            statusCode(200)
        }
        Given {
            queryParam("profile", profile)
        } When {
            post("/management/lists")
        } Then {
            statusCode(409)
        }
    }

    private fun addCredential(): ValidatableResponse {
        return Given {
            pathParam("profileName", profile)
        } When {
            post("/management/lists/{profileName}/entry")
        } Then {
            statusCode(200)
        }
    }

    private fun addEntry(profile: String): String {
        return Given {
            pathParam("profileName", profile)
        } When {
            post("/management/lists/{profileName}/entry")
        } Then {
            statusCode(200)
        } Extract {
            body().path("statusListIndex")
        }
    }

    private fun revokeEntry(profile: String, idx: String) {
        Given {
            pathParam("profileName", profile)
            pathParam("idx", idx)
        } When {
            delete("/management/lists/{profileName}/entry/{idx}")
        } Then {
            statusCode(204)
        }
    }

    private fun extractIndex(response: ValidatableResponse): Long {
        val indexStr: String = response.extract().path("statusListIndex")
        return indexStr.toLong()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00024")
    fun testAddCredential() {
        val resp = addCredential()
        resp.body("statusListCredential", `is`("https://revocation/status/$listName"))
            .body("id", `is`("https://revocation/status/$listName#${extractIndex(resp)}"))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00024")
    fun testCreateEmptyListCredential() {
        issueCredentials()

        Given {
            pathParam("listName", listName)
        } When {
            get("/status/{listName}")
        } Then {
            statusCode(200)
            this.body("$.subject.encodedList", listCredentialMatcher {
                val encoded: String = it.path("subject.encodedList")
                val listBytes = StatusBitSet.decodeBitset(encoded)
                listBytes.bitset.isEmpty
            })
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00024")
    fun testEmptyEncodedList() {
        issueCredentials()
        checkForEmptyEncodedList()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00024")
    fun testUpdatingOfEncodedList() {
        val numEntries = 100
        val stepSize = 2
        //var start = Instant.now()

        (0..numEntries).asSequence().asStream().parallel().forEach {
            addEntry(profile)
        }
        // here encoded list should be empty
        issueCredentials()

        checkForEmptyEncodedList()

        for (i in 0..numEntries step stepSize) {
            revokeEntry(profile, i.toString())
        }
        // here encoded list is updated with revoked items
        issueCredentials()

        Given {
            pathParam("profile", profile)
        } When {
            get("/management/lists/{profile}/encoded")
        } Then {
            statusCode(200)
        } Extract {
            val encoded: String = this.body().asString()
            val bitset = StatusBitSet.decodeBitset(encoded).bitset

            var correct = true
            for (i in 0..numEntries) {
                val expected = i % stepSize == 0
                if (bitset[i] != expected) {
                    correct = false
                }
            }
            correct
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00024")
    fun testCreateSmallListCredential() {
        val numEntries = 100
        val stepSize = 2
        //var start = Instant.now()
        (0..numEntries).asSequence().asStream().parallel().forEach {
            addEntry(profile)
        }
        //println(Duration.between(start, Instant.now()))
        //start = Instant.now()
        for (i in 0..numEntries step stepSize) {
            revokeEntry(profile, i.toString())
        }
        //println(Duration.between(start, Instant.now()))
        //start = Instant.now()

        issueCredentials()

        Given {
            pathParam("listName", listName)
        } When {
            get("/status/{listName}")
        } Then {
            statusCode(200)
            contentType("application/json")
            body("$.subject.encodedList", listCredentialMatcher {
                val encoded: String = it.path("subject.encodedList")
                val bitset = StatusBitSet.decodeBitset(encoded).bitset
                var correct = true
                for (i in 0..numEntries) {
                    val expected = i % stepSize == 0
                    if (bitset[i] != expected) {
                        correct = false
                    }
                }
                correct
            })
        }
        //println(Duration.between(start, Instant.now()))
    }

    private fun issueCredentials() {
        Given {
            pathParam("profileName", profile)
            queryParam("force", true)
        } When {
            post("/management/lists/issue-credential/{profileName}")
        } Then {
            statusCode(204)
        }
    }

    private fun checkForEmptyEncodedList() {
        Given {
            pathParam("profile", profile)
        } When {
            get("/management/lists/{profile}/encoded")
        } Then {
            statusCode(200)
        } Extract {
            val encoded: String = this.body().asString()
            val listBytes = StatusBitSet.decodeBitset(encoded)
            listBytes.bitset.isEmpty
        }
    }
}

fun listCredentialMatcher(body: (Response) -> Boolean): ResponseAwareMatcher<Response> {
    return ResponseAwareMatcher<Response> { response ->
        val correct = body(response!!)

        if (correct) {
            anything()
        } else {
            equalTo("impossible-value")
        }
    }
}


class IssueServiceMock : QuarkusTestResourceLifecycleManager {
    private var mockServer: WireMockServer = WireMockServer(
        options()
            .dynamicPort()
    )

    override fun start(): MutableMap<String, String> {
        mockServer.resetAll()
        mockServer.apply {
            start()

            stubFor(WireMock.post(
                WireMock.urlMatching("/list-credential/.*/issue"))
                .willReturn(WireMock.aResponse()
                    .withTransformers("response-template")
                    .withHeader("Content-Type", "application/json")
                    .withBody("{{{jsonPath request.body '$'}}}")
                )
            )
        }

        return mutableMapOf(("quarkus.rest-client.ssi-issuance-api.url" to mockServer.baseUrl()))
    }

    override fun stop() {
        mockServer.stop()
    }

}
