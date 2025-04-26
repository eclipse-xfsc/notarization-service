package eu.xfsc.not.ssi_issuance2

import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ServiceInitRequest
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.xfsc.not.ssi_issuance2.domain.ProfileProvider
import eu.xfsc.not.ssi_issuance2.mock.*
import io.quarkus.test.InjectMock
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.net.URI
import java.util.*

@QuarkusTest
@QuarkusTestResource(AcapyServiceMock::class)
@QuarkusTestResource(RevocationServiceMock::class)
@QuarkusTestResource(Oid4VciOfferApiMock::class)
class IssuanceResourceTest {

    @InjectMock
    lateinit var profileMock: ProfileProvider

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun testInit() {
        ProfileMock.setUpFetchJsonLDProfile(profileMock)

        val req = ServiceInitRequest(
            profileId = UUID.randomUUID().toString(),
            keyType = KeyType.ED25519,
            signatureType = SignatureType.ED25519SIGNATURE2018,
        )
        given()
            .contentType(ContentType.JSON)
            .body(req).log().all()
            .post("/api/v2/issuance/init-service")
            .then()
            .statusCode(200)
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun createAndDeleteIssuance() {

        val issuance = TestHelper.createIssuance()

        assertThat(issuance.cancelUrl, instanceOf(URI::class.java))
        assertThat("Cancel URL is wrong", issuance.cancelUrl.toString(), matchesPattern(".+issuance.+"))

        given()
            .delete(issuance.cancelUrl.path)
            .then()
            .statusCode(204)
    }
}
