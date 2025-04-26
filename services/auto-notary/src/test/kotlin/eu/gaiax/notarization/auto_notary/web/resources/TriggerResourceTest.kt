package eu.gaiax.notarization.auto_notary.web.resources

import com.github.tomakehurst.wiremock.WireMockServer
import eu.gaiax.notarization.auto_notary.web.client.MockAuthServer
import eu.gaiax.notarization.auto_notary.web.client.MockAuthServerResource
import eu.gaiax.notarization.auto_notary.web.client.MockRequestProcessing
import eu.gaiax.notarization.auto_notary.web.client.MockRequestProcessingResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import jakarta.inject.Inject
import org.hamcrest.Matchers
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

@QuarkusTest
@QuarkusTestResource(MockRequestProcessingResource::class)
@QuarkusTestResource(
    MockAuthServerResource::class
)
class TriggerResourceTest {
    @MockRequestProcessing
    lateinit var mockRequestProcessing: WireMockServer

    @MockAuthServer
    lateinit var mockAuthServer: WireMockServer

    @Inject
    lateinit var logger: Logger
    @BeforeEach
    fun setup() {
        mockRequestProcessing.resetAll()
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00019")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00027")
    fun testSomeAvailableTrigger() {
        val setup = RequestProcessingStub(mockRequestProcessing)
        setup.addCaimableRequest()
        setup.addCaimableRequest()
        setup.registerRequests()
        RestAssured.given()
            .`when`().post("/trigger/available")
            .then()
            .statusCode(200)
            .body("success", Matchers.equalTo(2), "failure", Matchers.equalTo(0))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00026")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00027")
    fun testSomeErrorTriggers() {
        val setup = RequestProcessingStub(mockRequestProcessing)
        setup.addCaimableRequest()
        setup.addFailingCaimableRequest()
        setup.addCaimableRequest()
        setup.addCaimableRequest()
        setup.addFailingCaimableRequest()
        setup.registerRequests()
        RestAssured.given()
            .`when`().post("/trigger/available")
            .then()
            .statusCode(200)
            .body("success", Matchers.equalTo(3), "failure", Matchers.equalTo(2))
    }
}
