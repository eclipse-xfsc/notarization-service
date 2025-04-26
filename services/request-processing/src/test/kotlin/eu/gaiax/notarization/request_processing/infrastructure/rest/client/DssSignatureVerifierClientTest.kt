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
package eu.gaiax.notarization.request_processing.infrastructure.rest.client

import eu.gaiax.notarization.RabbitMqTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.restassured.RestAssured
import jakarta.inject.Inject
import org.eclipse.microprofile.config.ConfigProvider
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junitpioneer.jupiter.ReportEntry
import java.io.IOException
import java.io.InputStream

/**
 *
 * @author Michael Rauh
 */
@QuarkusTest
@QuarkusTestResource(RabbitMqTestResourceLifecycleManager::class)
@EnabledIf(value = "isServiceAvailable", disabledReason = "The configured DSS service is currently unavailable!")
class DssSignatureVerifierClientTest {
    @Inject
    lateinit var signatureVerifierService: DssSignatureVerifierClient
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00036")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @RunOnVertxContext
    fun testValidSignedDocument(asserter: UniAsserter) {
        val rawEtsiReport = signatureVerifierService!!.verify(validSignedDocument!!)
        asserter.execute<ByteArray> {
            rawEtsiReport.invoke { report: ByteArray? ->
                val asString = String(report!!)
                MatcherAssert.assertThat(asString, Matchers.containsString("mainindication:passed"))
                MatcherAssert.assertThat(asString, Matchers.not(Matchers.containsString("mainindication:failed")))
            }
        }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00036")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00037")
    @RunOnVertxContext
    fun testInValidSignedDocument(asserter: UniAsserter) {
        val rawEtsiReport = signatureVerifierService.verify(invalidSignedDocument!!)
        asserter.execute<ByteArray> {
            rawEtsiReport.invoke { report: ByteArray? ->
                val asString = String(report!!)
                MatcherAssert.assertThat(asString, Matchers.containsString("total-failed"))
                MatcherAssert.assertThat(asString, Matchers.containsString("HASH_FAILURE"))
            }
        }
    }

    companion object {
        private var validSignedDocument: InputStream? = null
        private var invalidSignedDocument: InputStream? = null
        @JvmStatic
        @BeforeAll
        @Throws(IOException::class)
        fun init() {
            validSignedDocument =
                DssSignatureVerifierClientTest::class.java.getResourceAsStream("/valid-signed-document.xml")
            invalidSignedDocument =
                DssSignatureVerifierClientTest::class.java.getResourceAsStream("/invalid-xades-structure.xml")
        }

        @JvmStatic
        fun isServiceAvailable(): Boolean {
            val url = ConfigProvider.getConfig().getValue("%test.quarkus.rest-client.dss-api.url", String::class.java)

            val response = RestAssured.get(url).thenReturn()
            return response.statusCode() != 503
        }
    }
}
